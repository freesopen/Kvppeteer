package top.netapps.kvppeteer.util

import java.time.LocalDateTime

object StringUtil {
    fun isEmpty(s: String?): Boolean {
        return s == null || s.isEmpty()
    }

    fun isNotEmpty(s: String?): Boolean {
        return !isEmpty(s)
    }

    fun isBlank(str: String?): Boolean {
        var strLen: Int = 0
        if (str == null || str.length.also { strLen = it } == 0) {
            return true
        }
        for (i in 0 until strLen) {
            if (!Character.isWhitespace(str[i])) {
                return false
            }
        }
        return true
    }

    fun isNotBlank(str: String?): Boolean {
        return !isBlank(str)
    }

    val timestamp: Int
        get() {
            synchronized(StringUtil::class.java) { return LocalDateTime.now().nano }
        }
}