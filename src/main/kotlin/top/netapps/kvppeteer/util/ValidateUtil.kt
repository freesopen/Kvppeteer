package top.netapps.kvppeteer.util

/**
 * 验证类工具
 */
object ValidateUtil {
    /**
     * 集合是否为空
     * @param c 集合
     * @return 结果
     */
    fun isEmpty(c: Collection<*>?): Boolean {
        return c == null || c.isEmpty()
    }

    /**
     * 集合是否不为空
     * @param c 集合
     * @return 结果
     */
    fun isNotEmpty(c: Collection<*>?): Boolean {
        return !isEmpty(c)
    }

    /**
     * 判断
     * @param object 要判空的对象
     * @param message 提示信息
     */
    fun notNull(`object`: Any?, message: String?) {
        if (`object` == null) {
            throw NullPointerException(message)
        }
    }

    /**
     * 断言参数是否
     * @param condition 断言失败是false 会抛异常
     * @param errorText 异常信息提示
     */
    fun assertArg(condition: Boolean, errorText: String) {
        require(condition) { errorText }
    }
}