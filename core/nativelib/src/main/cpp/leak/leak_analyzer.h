#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

struct LeakTopDomain {
    std::string domain;
    int64_t count = 0;
    int64_t entropySuspicious = 0;
    int64_t burst = 0;
};

struct LeakTopServer {
    std::string ip;
    int64_t count = 0;
    int64_t publicCount = 0;
};

struct LeakSnapshot {
    int32_t score = 0;

    int64_t windowMs = 600000;
    int64_t nowMs = 0;

    int64_t totalQueries = 0;
    int32_t uniqueDomains = 0;

    double publicDnsRatio = 0.0;
    int64_t publicDnsQueries = 0;

    int64_t suspiciousEntropyQueries = 0;
    int64_t burstQueries = 0;

    std::vector<LeakTopDomain> topDomains;
    std::vector<LeakTopServer> topServers;

    std::string json;
};

class LeakAnalyzerImpl;

class LeakAnalyzer {
public:
    explicit LeakAnalyzer(int64_t windowMs);
    ~LeakAnalyzer();

    LeakAnalyzer(const LeakAnalyzer&) = delete;
    LeakAnalyzer& operator=(const LeakAnalyzer&) = delete;

    LeakAnalyzer(LeakAnalyzer&&) noexcept;
    LeakAnalyzer& operator=(LeakAnalyzer&&) noexcept;

    void setWindowMs(int64_t windowMs);
    void reset();

    void onDns(int64_t tsMs, int32_t uid, const std::string& qname, int32_t qtype, const std::string& serverIp);

    LeakSnapshot snapshot(int32_t topN);

    static bool isSuspiciousEntropy(const std::string& domain);
    static bool isPublicDns(const std::string& ip);
    static std::string normalizeDomain(const std::string& qname);

private:
    std::unique_ptr<LeakAnalyzerImpl> impl_;

    static double shannonEntropy(const std::string& s);
};
