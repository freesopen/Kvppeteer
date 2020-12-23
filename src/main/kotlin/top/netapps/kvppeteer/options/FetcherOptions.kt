package top.netapps.kvppeteer.options

class FetcherOptions {

    var platform: String? = null
    var path: String? = null
    var host: String? = null
    var product: String? = null

    constructor() : super()
    constructor(platform: String?, path: String?, host: String?, product: String?) : super() {
        this.platform = platform
        this.path = path
        this.host = host
        this.product = product
    }

}