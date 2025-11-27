package com.muratcangzm

interface AppLogger {
    fun d(message: String, throwable: Throwable? = null)
    fun i(message: String, throwable: Throwable? = null)
    fun w(message: String, throwable: Throwable? = null)
    fun e(message: String, throwable: Throwable? = null)
}

object Logger : AppLogger {

    @Volatile
    private var delegate: AppLogger? = null

    fun init(delegate: AppLogger) {
        this.delegate = delegate
    }

    override fun d(message: String, throwable: Throwable?) {
        delegate?.d(message, throwable)
    }

    override fun i(message: String, throwable: Throwable?) {
        delegate?.i(message, throwable)
    }

    override fun w(message: String, throwable: Throwable?) {
        delegate?.w(message, throwable)
    }

    override fun e(message: String, throwable: Throwable?) {
        delegate?.e(message, throwable)
    }
}