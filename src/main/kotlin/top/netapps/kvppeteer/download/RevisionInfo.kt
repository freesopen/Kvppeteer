package top.netapps.kvppeteer.download

class RevisionInfo {
    var revision: String? = null
    var executablePath: String? = null
    var folderPath: String? = null
    var local = false
    var url: String? = null
    var product: String? = null

    constructor(revision: String?, executablePath: String?,
                folderPath: String?, local: Boolean, url: String?, product: String?) {
        this.revision = revision
        this.executablePath = executablePath
        this.folderPath = folderPath
        this.local = local
        this.url = url
        this.product = product
    }
}