package com.zenlauncher.zenmode

import com.zenlauncher.zenmode.ui.screens.CopyLine
import java.io.File

/**
 * Resolves a copy resource the way Android would, so a plain unit test can assert
 * the exact sentence a screen shows.
 *
 * The resource id is mapped back to its name through the generated R class, the
 * value is read straight out of res/values/strings.xml and unescaped the way aapt2
 * would, and the arguments are substituted with [String.format] - the same call
 * Resources.getString makes.
 */
object TestStrings {

    /** Resource id to resource name, read off the generated R class. */
    private val namesById: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    /** Resource name to its value, straight from the source tree. */
    private val valuesByName: Map<String, String> by lazy { load() }

    /** The words a screen shows for [line]. */
    fun format(line: CopyLine): String = format(
        line.res,
        *line.args.map { if (it is CopyLine) format(it) else it }.toTypedArray()
    )

    /** The words a screen shows for the resource [resId]. */
    fun format(resId: Int, vararg args: Any): String {
        val name = namesById[resId] ?: error("unknown string resource id $resId")
        val raw = valuesByName[name] ?: error("no string resource named $name")
        return String.format(raw, *args)
    }

    private fun load(): Map<String, String> {
        val text = stringsFile().readText()
        val out = HashMap<String, String>()
        val pattern = Regex("<string name=\"([^\"]+)\"([^>]*)>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        for (match in pattern.findAll(text)) {
            val formatted = !match.groupValues[2].contains("formatted=\"false\"")
            out[match.groupValues[1]] = unescape(match.groupValues[3], formatted)
        }
        return out
    }

    /** The unit test's working directory is the module directory, so look upwards. */
    private fun stringsFile(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, "src/main/res/values/strings.xml")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        error("could not find src/main/res/values/strings.xml")
    }

    /** Undo what aapt2 does to a string resource's value. */
    private fun unescape(value: String, formatted: Boolean): String {
        var s = value
        s = s.replace("&lt;", "<").replace("&gt;", ">")
        s = s.replace("&quot;", "\"").replace("&apos;", "'")
        s = s.replace("&amp;", "&")
        // A quoted value keeps its whitespace exactly; an unquoted one is trimmed and
        // its runs of internal whitespace collapse to a single space.
        if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
        } else if (!formatted) {
            s = s.trim().replace(Regex("\\s+"), " ")
        }
        s = s.replace("\\n", "\n").replace("\\t", "\t")
        s = s.replace("\\'", "'").replace("\\\"", "\"")
        return s
    }
}
