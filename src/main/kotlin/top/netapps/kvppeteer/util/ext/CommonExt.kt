//package top.netapps.kvppeteer.util.ext
//
///**
// * @ClassName: CommonExt
// * @author: freesopen
// * @date: 2020/11/14  23:36
// */
//
///**
// * 集合是否为空
// * @param c 集合
// * @return 结果
// */
//fun isEmpty(c: Collection<*>?): Boolean {
//    return c == null || c.isEmpty()
//}
//
///**
// * 集合是否不为空
// * @param c 集合
// * @return 结果
// */
//fun isNotEmpty(c: Collection<*>?): Boolean {
//    return !isEmpty(c)
//}
//
///**
// * 判断
// * @param object 要判空的对象
// * @param message 提示信息
// */
//fun notNull(obj: Any?, message: String?) {
//    if (obj == null) {
//        throw NullPointerException(message)
//    }
//}
//
///**
// * 断言参数是否
// * @param condition 断言失败是false 会抛异常
// * @param errorText 异常信息提示
// */
//fun assertArg(condition: Boolean, errorText: String) {
//    require(condition) { errorText }
//}
//fun isEmpty(s:String):Boolean{
//    return s.isEmpty()
//}
//fun isNotEmpty(s:String?):Boolean{
//    return s?.isEmpty() ?: false
//}
//fun main() {
//    println(    isNotEmpty(""))
//
//}