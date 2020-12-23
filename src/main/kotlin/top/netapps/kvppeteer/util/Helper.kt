package top.netapps.kvppeteer.util

import org.eclipse.jetty.util.StringUtil
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.regex.Pattern

/**
 * 一些公共方法
 */
object Helper {
    /**
     * 单线程，一个浏览器只能有一个trcing 任务
     */
    private val COMMON_EXECUTOR: ExecutorService? = null
    private val LOGGER = LoggerFactory.getLogger(Helper::class.java)
    private val os = System.getProperty("os.name")

    /**
     * Returns true if the operating system is a form of Windows.
     * @return windows return  true
     */
    val isWindows = os.startsWith("Windows")

    /**
     * Returns true if the operating system is a form of Mac OS.
     * @return mac return  true
     */
    val isMac = os.startsWith("Mac")

    /**
     * Returns true if the operating system is a form of Linux.
     * @return linux return  true
     */
    val isLinux = os.startsWith("Linux")

    /**
     *
     * 是否是win64
     * @return true is win64
     */
    val isWin64: Boolean
        get() {
            val arch = System.getProperty("os.arch")
            return arch.contains("64")
        }

    fun paltform(): String {
        return System.getProperty("os.name")
    }

    @Throws(IOException::class)
    fun chmod(path: String?, perms: String) {
        require(!StringUtil.isEmpty(path)) { "Path must not be empty" }
        val chars = perms.toCharArray()
        require(chars.size == 3) { "perms length must be 3" }
        val path1 = Paths.get(path)
        val permissions: MutableSet<PosixFilePermission> = HashSet()
        //own
        if ('1' == chars[0]) {
            permissions.add(PosixFilePermission.OWNER_EXECUTE)
        } else if ('2' == chars[0]) {
            permissions.add(PosixFilePermission.OWNER_WRITE)
        } else if ('3' == chars[0]) {
            permissions.add(PosixFilePermission.OWNER_WRITE)
            permissions.add(PosixFilePermission.OWNER_EXECUTE)
        } else if ('4' == chars[0]) {
            permissions.add(PosixFilePermission.OWNER_READ)
        } else if ('5' == chars[0]) {
            permissions.add(PosixFilePermission.OWNER_READ)
            permissions.add(PosixFilePermission.OWNER_EXECUTE)
        } else if ('6' == chars[0]) {
            permissions.add(PosixFilePermission.OWNER_READ)
            permissions.add(PosixFilePermission.OWNER_WRITE)
        } else if ('7' == chars[0]) {
            permissions.add(PosixFilePermission.OWNER_READ)
            permissions.add(PosixFilePermission.OWNER_WRITE)
            permissions.add(PosixFilePermission.OWNER_EXECUTE)
        }
        //group
        if ('1' == chars[1]) {
            permissions.add(PosixFilePermission.GROUP_EXECUTE)
        } else if ('2' == chars[1]) {
            permissions.add(PosixFilePermission.GROUP_WRITE)
        } else if ('3' == chars[1]) {
            permissions.add(PosixFilePermission.GROUP_WRITE)
            permissions.add(PosixFilePermission.GROUP_EXECUTE)
        } else if ('4' == chars[1]) {
            permissions.add(PosixFilePermission.GROUP_READ)
        } else if ('5' == chars[1]) {
            permissions.add(PosixFilePermission.GROUP_READ)
            permissions.add(PosixFilePermission.GROUP_EXECUTE)
        } else if ('6' == chars[1]) {
            permissions.add(PosixFilePermission.GROUP_READ)
            permissions.add(PosixFilePermission.GROUP_WRITE)
        } else if ('7' == chars[1]) {
            permissions.add(PosixFilePermission.GROUP_READ)
            permissions.add(PosixFilePermission.GROUP_WRITE)
            permissions.add(PosixFilePermission.GROUP_EXECUTE)
        }
        //other
        if ('1' == chars[2]) {
            permissions.add(PosixFilePermission.OTHERS_EXECUTE)
        } else if ('2' == chars[2]) {
            permissions.add(PosixFilePermission.OTHERS_WRITE)
        } else if ('3' == chars[2]) {
            permissions.add(PosixFilePermission.OTHERS_WRITE)
            permissions.add(PosixFilePermission.OTHERS_EXECUTE)
        } else if ('4' == chars[2]) {
            permissions.add(PosixFilePermission.OTHERS_READ)
        } else if ('5' == chars[2]) {
            permissions.add(PosixFilePermission.OTHERS_READ)
            permissions.add(PosixFilePermission.OTHERS_EXECUTE)
        } else if ('6' == chars[2]) {
            permissions.add(PosixFilePermission.OTHERS_READ)
            permissions.add(PosixFilePermission.OWNER_WRITE)
        } else if ('7' == chars[2]) {
            permissions.add(PosixFilePermission.OTHERS_READ)
            permissions.add(PosixFilePermission.OTHERS_WRITE)
            permissions.add(PosixFilePermission.OTHERS_EXECUTE)
        }
        Files.setPosixFilePermissions(path1, permissions)
    }

    fun join(root: String?, vararg args: String?): String {
        return Paths.get(root, *args).toString()
    }

    /**
     * 多个字节数组转成一个字节数组
     * @param bufs 数组集合
     * @param byteLength 数组总长度
     * @return 总数组
     */
    private fun getBytes(bufs: List<ByteArray>, byteLength: Int): ByteArray {
        //返回字节数组
        val resultBuf = ByteArray(byteLength)
        var destPos = 0
        for (buf in bufs) {
            System.arraycopy(buf, 0, resultBuf, destPos, buf.size)
            destPos += buf.size
        }
        return resultBuf
    }

    fun isString(value: Any?): Boolean {
        return if (value == null) false else value.javaClass == String::class.java
    }

    fun isNumber(s: String?): Boolean {
        val pattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?")
        val matcher = pattern.matcher(s)
        return matcher.matches()
    }

    /**
     * 判断js字符串是否是一个函数
     * @param pageFunction js字符串
     * @return true代表是js函数
     */
    fun isFunction(pageFunction: String): Boolean {
        var pageFunction = pageFunction
        pageFunction = pageFunction.trim { it <= ' ' }
        return pageFunction.startsWith("function") || pageFunction.startsWith("async") || pageFunction.contains("=>")
    }
}