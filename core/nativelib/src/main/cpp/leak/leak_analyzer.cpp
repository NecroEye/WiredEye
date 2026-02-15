#include "leak_analyzer.h"

#include <algorithm>
#include <cmath>
#include <deque>
#include <mutex>
#include <sstream>
#include <unordered_map>

namespace {

    struct Event {
        int64_t tsMs;
        int32_t domainId;
        int32_t serverId;
        bool isPublicDns;
        bool isEntropySuspicious;
        bool isBurst;
    };

    struct DomainAgg {
        int64_t count = 0;
        int64_t entropySuspicious = 0;
        int64_t burst = 0;
        std::deque<int64_t> recentTs;
    };

    struct ServerAgg {
        int64_t count = 0;
        int64_t publicCount = 0;
    };

    static constexpr int64_t BURST_WINDOW_MS = 2500;
    static constexpr int32_t BURST_THRESHOLD = 10;

    static constexpr double ENTROPY_THRESHOLD = 3.60;
    static constexpr int32_t ENTROPY_MIN_LEN = 18;

    static std::string toLower(std::string s) {
        for (char& c : s) {
            if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
        }
        return s;
    }

    static std::string jsonEscape(const std::string& in) {
        std::ostringstream o;
        for (char c : in) {
            switch (c) {
                case '\"': o << "\\\""; break;
                case '\\': o << "\\\\"; break;
                case '\b': o << "\\b"; break;
                case '\f': o << "\\f"; break;
                case '\n': o << "\\n"; break;
                case '\r': o << "\\r"; break;
                case '\t': o << "\\t"; break;
                default:
                    if (static_cast<unsigned char>(c) < 0x20) {
                        o << "\\u"
                          << std::hex << std::uppercase
                          << ((static_cast<int>(static_cast<unsigned char>(c)) >> 4) & 0xF)
                          << (static_cast<int>(static_cast<unsigned char>(c)) & 0xF);
                    } else {
                        o << c;
                    }
            }
        }
        return o.str();
    }

} // namespace

class LeakAnalyzerImpl {
public:
    explicit LeakAnalyzerImpl(int64_t windowMs) : windowMs_(windowMs <= 0 ? 600000 : windowMs) {}

    void setWindowMs(int64_t windowMs) {
        std::lock_guard<std::mutex> lg(mu_);
        windowMs_ = std::max<int64_t>(1000, windowMs);
    }

    void reset() {
        std::lock_guard<std::mutex> lg(mu_);
        events_.clear();
        domainMap_.clear();
        serverMap_.clear();
        domainAgg_.clear();
        serverAgg_.clear();
        idToDomain_.clear();
        idToServer_.clear();
        total_ = 0;
        publicDns_ = 0;
        entropySus_ = 0;
        burst_ = 0;
    }

    void onDns(int64_t tsMs, int32_t, const std::string& qname, int32_t, const std::string& serverIp) {
        std::lock_guard<std::mutex> lg(mu_);

        const int64_t nowMs = tsMs;
        evictOldLocked(nowMs);

        const std::string domain = LeakAnalyzer::normalizeDomain(qname);
        if (domain.empty()) return;

        const int32_t dId = internDomainLocked(domain);
        const int32_t sId = internServerLocked(serverIp);

        auto& dAgg = domainAgg_[dId];
        auto& sAgg = serverAgg_[sId];

        const bool pub = LeakAnalyzer::isPublicDns(serverIp);
        const bool entropy = LeakAnalyzer::isSuspiciousEntropy(domain);

        bool burstNow = false;
        {
            auto& dq = dAgg.recentTs;
            dq.push_back(tsMs);
            while (!dq.empty() && (tsMs - dq.front()) > BURST_WINDOW_MS) dq.pop_front();
            if (static_cast<int32_t>(dq.size()) >= BURST_THRESHOLD) burstNow = true;
        }

        dAgg.count += 1;
        if (entropy) dAgg.entropySuspicious += 1;
        if (burstNow) dAgg.burst += 1;

        sAgg.count += 1;
        if (pub) sAgg.publicCount += 1;

        total_ += 1;
        if (pub) publicDns_ += 1;
        if (entropy) entropySus_ += 1;
        if (burstNow) burst_ += 1;

        events_.push_back(Event{
                .tsMs = tsMs,
                .domainId = dId,
                .serverId = sId,
                .isPublicDns = pub,
                .isEntropySuspicious = entropy,
                .isBurst = burstNow
        });
    }

    LeakSnapshot snapshot(int32_t topN) {
        std::lock_guard<std::mutex> lg(mu_);

        const int64_t nowMs = events_.empty() ? 0 : events_.back().tsMs;
        evictOldLocked(nowMs);

        LeakSnapshot out;
        out.windowMs = windowMs_;
        out.nowMs = nowMs;
        out.totalQueries = total_;
        out.publicDnsQueries = publicDns_;
        out.suspiciousEntropyQueries = entropySus_;
        out.burstQueries = burst_;
        out.uniqueDomains = static_cast<int32_t>(domainAgg_.size());
        out.publicDnsRatio = (total_ <= 0) ? 0.0 : static_cast<double>(publicDns_) / static_cast<double>(total_);

        int score = 0;
        score += static_cast<int>(std::round(std::min(1.0, out.publicDnsRatio / 0.50) * 25.0));

        const double burstRatio = (total_ <= 0) ? 0.0 : static_cast<double>(burst_) / static_cast<double>(total_);
        score += static_cast<int>(std::round(std::min(1.0, burstRatio / 0.10) * 25.0));

        const double entRatio = (total_ <= 0) ? 0.0 : static_cast<double>(entropySus_) / static_cast<double>(total_);
        score += static_cast<int>(std::round(std::min(1.0, entRatio / 0.15) * 20.0));

        score += static_cast<int>(std::round(std::min(1.0, out.uniqueDomains / 200.0) * 15.0));
        score += static_cast<int>(std::round(std::min(1.0, out.totalQueries / 5000.0) * 15.0));
        out.score = std::min(100, std::max(0, score));

        std::vector<std::pair<int32_t, int64_t>> dom;
        dom.reserve(domainAgg_.size());
        for (auto& kv : domainAgg_) dom.emplace_back(kv.first, kv.second.count);
        std::sort(dom.begin(), dom.end(), [](auto& a, auto& b) { return a.second > b.second; });

        std::vector<std::pair<int32_t, int64_t>> srv;
        srv.reserve(serverAgg_.size());
        for (auto& kv : serverAgg_) srv.emplace_back(kv.first, kv.second.count);
        std::sort(srv.begin(), srv.end(), [](auto& a, auto& b) { return a.second > b.second; });

        const int nDom = std::max(0, std::min<int>(topN, static_cast<int>(dom.size())));
        const int nSrv = std::max(0, std::min<int>(topN, static_cast<int>(srv.size())));

        out.topDomains.clear();
        out.topDomains.reserve(nDom);
        for (int i = 0; i < nDom; i++) {
            const int32_t id = dom[i].first;
            const auto& name = idToDomain_[id];
            const auto& agg = domainAgg_[id];
            out.topDomains.push_back(LeakTopDomain{
                    .domain = name,
                    .count = agg.count,
                    .entropySuspicious = agg.entropySuspicious,
                    .burst = agg.burst
            });
        }

        out.topServers.clear();
        out.topServers.reserve(nSrv);
        for (int i = 0; i < nSrv; i++) {
            const int32_t id = srv[i].first;
            const auto& ip = idToServer_[id];
            const auto& agg = serverAgg_[id];
            out.topServers.push_back(LeakTopServer{
                    .ip = ip,
                    .count = agg.count,
                    .publicCount = agg.publicCount
            });
        }

        std::ostringstream json;
        json << "{";
        json << "\"windowMs\":" << out.windowMs << ",";
        json << "\"nowMs\":" << out.nowMs << ",";
        json << "\"score\":" << out.score << ",";
        json << "\"totalQueries\":" << out.totalQueries << ",";
        json << "\"uniqueDomains\":" << out.uniqueDomains << ",";
        json << "\"publicDnsRatio\":" << out.publicDnsRatio << ",";
        json << "\"publicDnsQueries\":" << out.publicDnsQueries << ",";
        json << "\"suspiciousEntropyQueries\":" << out.suspiciousEntropyQueries << ",";
        json << "\"burstQueries\":" << out.burstQueries << ",";

        json << "\"topDomains\":[";
        for (int i = 0; i < nDom; i++) {
            const auto& t = out.topDomains[i];
            if (i) json << ",";
            json << "{"
                 << "\"domain\":\"" << jsonEscape(t.domain) << "\","
                 << "\"count\":" << t.count << ","
                 << "\"entropySuspicious\":" << t.entropySuspicious << ","
                 << "\"burst\":" << t.burst
                 << "}";
        }
        json << "],";

        json << "\"topServers\":[";
        for (int i = 0; i < nSrv; i++) {
            const auto& t = out.topServers[i];
            if (i) json << ",";
            json << "{"
                 << "\"ip\":\"" << jsonEscape(t.ip) << "\","
                 << "\"count\":" << t.count << ","
                 << "\"publicCount\":" << t.publicCount
                 << "}";
        }
        json << "]";

        json << "}";

        out.json = json.str();
        return out;
    }

private:
    void evictOldLocked(int64_t nowMs) {
        if (windowMs_ <= 0) return;
        const int64_t cutoff = nowMs - windowMs_;

        while (!events_.empty() && events_.front().tsMs < cutoff) {
            const Event e = events_.front();
            events_.pop_front();

            total_ -= 1;
            if (e.isPublicDns) publicDns_ -= 1;
            if (e.isEntropySuspicious) entropySus_ -= 1;
            if (e.isBurst) burst_ -= 1;

            auto dIt = domainAgg_.find(e.domainId);
            if (dIt != domainAgg_.end()) {
                dIt->second.count -= 1;
                if (e.isEntropySuspicious) dIt->second.entropySuspicious -= 1;
                if (e.isBurst) dIt->second.burst -= 1;

                auto& dq = dIt->second.recentTs;
                while (!dq.empty() && (nowMs - dq.front()) > BURST_WINDOW_MS) dq.pop_front();

                if (dIt->second.count <= 0) domainAgg_.erase(dIt);
            }

            auto sIt = serverAgg_.find(e.serverId);
            if (sIt != serverAgg_.end()) {
                sIt->second.count -= 1;
                if (e.isPublicDns) sIt->second.publicCount -= 1;
                if (sIt->second.count <= 0) serverAgg_.erase(sIt);
            }
        }

        if (total_ < 0) total_ = 0;
        if (publicDns_ < 0) publicDns_ = 0;
        if (entropySus_ < 0) entropySus_ = 0;
        if (burst_ < 0) burst_ = 0;
    }

    int32_t internDomainLocked(const std::string& domain) {
        auto it = domainMap_.find(domain);
        if (it != domainMap_.end()) return it->second;
        const int32_t id = static_cast<int32_t>(idToDomain_.size());
        domainMap_[domain] = id;
        idToDomain_.push_back(domain);
        return id;
    }

    int32_t internServerLocked(const std::string& ip) {
        auto it = serverMap_.find(ip);
        if (it != serverMap_.end()) return it->second;
        const int32_t id = static_cast<int32_t>(idToServer_.size());
        serverMap_[ip] = id;
        idToServer_.push_back(ip);
        return id;
    }

private:
    std::mutex mu_;
    int64_t windowMs_;

    std::deque<Event> events_;

    std::unordered_map<std::string, int32_t> domainMap_;
    std::unordered_map<std::string, int32_t> serverMap_;

    std::unordered_map<int32_t, DomainAgg> domainAgg_;
    std::unordered_map<int32_t, ServerAgg> serverAgg_;

    std::vector<std::string> idToDomain_;
    std::vector<std::string> idToServer_;

    int64_t total_ = 0;
    int64_t publicDns_ = 0;
    int64_t entropySus_ = 0;
    int64_t burst_ = 0;
};

LeakAnalyzer::LeakAnalyzer(int64_t windowMs) : impl_(std::make_unique<LeakAnalyzerImpl>(windowMs)) {}
LeakAnalyzer::~LeakAnalyzer() = default;

LeakAnalyzer::LeakAnalyzer(LeakAnalyzer&&) noexcept = default;
LeakAnalyzer& LeakAnalyzer::operator=(LeakAnalyzer&&) noexcept = default;

void LeakAnalyzer::setWindowMs(int64_t windowMs) { impl_->setWindowMs(windowMs); }
void LeakAnalyzer::reset() { impl_->reset(); }
void LeakAnalyzer::onDns(int64_t tsMs, int32_t uid, const std::string& qname, int32_t qtype, const std::string& serverIp) {
    impl_->onDns(tsMs, uid, qname, qtype, serverIp);
}
LeakSnapshot LeakAnalyzer::snapshot(int32_t topN) { return impl_->snapshot(topN); }

std::string LeakAnalyzer::normalizeDomain(const std::string& qname) {
    std::string s = toLower(qname);
    while (!s.empty() && s.back() == '.') s.pop_back();
    if (s.size() < 2) return "";
    s.erase(std::remove_if(s.begin(), s.end(), [](char c) { return c <= 0x20; }), s.end());
    return s;
}

double LeakAnalyzer::shannonEntropy(const std::string& s) {
    if (s.empty()) return 0.0;
    int freq[256] = {0};
    for (unsigned char c : s) freq[c]++;
    const double len = static_cast<double>(s.size());
    double ent = 0.0;
    for (int f : freq) {
        if (f <= 0) continue;
        const double p = static_cast<double>(f) / len;
        ent -= p * std::log2(p);
    }
    return ent;
}

bool LeakAnalyzer::isSuspiciousEntropy(const std::string& domain) {
    if (static_cast<int32_t>(domain.size()) < ENTROPY_MIN_LEN) return false;

    const auto dot = domain.find('.');
    const std::string label = (dot == std::string::npos) ? domain : domain.substr(0, dot);

    if (static_cast<int32_t>(label.size()) < ENTROPY_MIN_LEN) return false;
    const double e = shannonEntropy(label);
    return e >= ENTROPY_THRESHOLD;
}

bool LeakAnalyzer::isPublicDns(const std::string& ip) {
    const std::string s = ip;
    return
            s == "8.8.8.8" || s == "8.8.4.4" ||
            s == "1.1.1.1" || s == "1.0.0.1" ||
            s == "9.9.9.9" || s == "149.112.112.112" ||
            s == "208.67.222.222" || s == "208.67.220.220" ||
            s == "94.140.14.14" || s == "94.140.15.15" ||
            s == "2606:4700:4700::1111" || s == "2606:4700:4700::1001" ||
            s == "2001:4860:4860::8888" || s == "2001:4860:4860::8844" ||
            s == "2620:fe::fe" || s == "2620:fe::9";
}
