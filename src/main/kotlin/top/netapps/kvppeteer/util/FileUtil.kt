package top.netapps.kvppeteer.util

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 操作文件的一些公告方法
 */
object FileUtil {
    /**
     * 根据给定的前缀创建临时文件夹
     * @param prefix 临时文件夹前缀
     * @return 临时文件夹路径
     */
    fun createProfileDir(prefix: String): String {
        return try {
            Files.createTempDirectory(prefix).toRealPath().toString()
        } catch (e: Exception) {
            throw RuntimeException("create temp profile dir fail:", e)
        }
    }

    /**
     * 断言路径是否是可执行的exe文件
     * @param executablePath 要断言的文件
     * @return 可执行，返回true
     */
    fun assertExecutable(executablePath: String): Boolean {
        val path = Paths.get(executablePath)
        return Files.isRegularFile(path) && Files.isReadable(path) && Files.isExecutable(path)
    }

    /**
     * 移除文件
     * @param path 要移除的路径
     */
    fun removeFolder(path: String) {
        val file = File(path)
        delete(file)
    }

    private fun delete(file: File) {
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null && files.isNotEmpty()) {
                for (f in files) {
                    delete(f)
                }
            }
            file.deleteOnExit()
        } else {
            file.deleteOnExit()
        }
    }

    /**
     * 创建一个文件，如果该文件上的有些文件夹路径不存在，会自动创建文件夹。
     * @param file 创建的文件
     * @throws IOException 异常
     */
    @Throws(IOException::class)
    fun createNewFile(file: File) {
        if (!file.exists()) {
            mkdir(file.parentFile)
            file.createNewFile()
        }
    }

    /**
     * 递归创建文件夹
     * @param parent 要创建的文件夹
     */
    fun mkdir(parent: File) {
        if (parent != null && !parent.exists()) {
            mkdir(parent.parentFile)
            parent.mkdir()
        }
    }
}