package com.example.data.skills

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class SkillCodeSnippet(
    val language: String,
    val code: String
)

data class SkillReferenceDoc(
    val fileName: String,
    val path: String,
    val content: String,
    val codeBlocks: List<SkillCodeSnippet>
)

data class SkillScriptTool(
    val fileName: String,
    val path: String,
    val content: String
)

data class ExtractedSkillItem(
    val id: String,
    val titleAr: String,
    val category: String,
    val archivePath: String,
    val descriptionEn: String,
    val keywords: List<String>,
    val skillMarkdown: String,
    val packages: List<String>,
    val cliCommands: List<String>,
    val codeSnippets: List<SkillCodeSnippet>,
    val references: List<SkillReferenceDoc>,
    val scripts: List<SkillScriptTool>,
    val isPinnedByDefault: Boolean
)

data class GlobalToolConfigItem(
    val name: String,
    val path: String,
    val type: String,
    val content: String
)

data class SkillsMemoryCatalog(
    val archiveName: String,
    val totalArchiveFiles: Int,
    val totalSkills: Int,
    val mcpServerUrl: String,
    val skills: List<ExtractedSkillItem>,
    val globalToolsAndConfigs: List<GlobalToolConfigItem>,
    val allFilePaths: List<String>
)

/**
 * Loads optional skill catalog and ZIP assets when bundled. Missing assets produce an empty catalog
 * instead of reporting files or skills that are not present.
 */
object SkillsMemoryEngine {

    private const val TAG = "SkillsMemoryEngine"
    private const val CATALOG_ASSET_NAME = "skills_memory_catalog.json"
    private const val ZIP_ASSET_NAME = "skills-main.zip"
    const val FIRESTORE_MEMORY_COLLECTION_NAME = "expo_skills_memory_registry"

    @Volatile
    private var cachedCatalog: SkillsMemoryCatalog? = null

    fun loadCatalog(context: Context): SkillsMemoryCatalog {
        cachedCatalog?.let { return it }
        synchronized(this) {
            cachedCatalog?.let { return it }
            return try {
                val jsonText = context.assets.open(CATALOG_ASSET_NAME).use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                }
                val parsed = parseCatalogJson(jsonText)
                cachedCatalog = parsed
                parsed
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load skills_memory_catalog.json from assets", e)
                SkillsMemoryCatalog(
                    archiveName = "",
                    totalArchiveFiles = 0,
                    totalSkills = 0,
                    mcpServerUrl = "",
                    skills = emptyList(),
                    globalToolsAndConfigs = emptyList(),
                    allFilePaths = emptyList()
                )
            }
        }
    }

    /**
     * Reads any raw file directly from the bundled `skills-main.zip` archive by its path.
     */
    fun readRawArchiveFile(context: Context, targetPath: String): String {
        return try {
            context.assets.open(ZIP_ASSET_NAME).use { rawStream ->
                ZipInputStream(rawStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == targetPath || entry.name.endsWith(targetPath)) {
                            return BufferedReader(InputStreamReader(zis, Charsets.UTF_8)).readText()
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            "// File not found in archive: $targetPath"
        } catch (e: Exception) {
            Log.e(TAG, "Error reading raw file $targetPath from $ZIP_ASSET_NAME", e)
            "// Error reading $targetPath: ${e.localizedMessage}"
        }
    }

    /**
     * Automatically selects the most relevant skills for a user prompt or code snippet,
     * combining user-pinned skills with keyword/intent-matched skills.
     */
    fun matchRelevantSkills(
        queryOrCode: String,
        pinnedSkillIds: Set<String>,
        allSkills: List<ExtractedSkillItem>,
        maxMatched: Int = 5
    ): List<ExtractedSkillItem> {
        if (allSkills.isEmpty() || maxMatched <= 0 || queryOrCode.isBlank()) return emptyList()
        val lower = queryOrCode.lowercase()

        val scored = allSkills.map { skill ->
            var score = 0
            if (skill.id in pinnedSkillIds) score += 25
            if (lower.contains(skill.id.lowercase())) score += 50
            if (lower.contains(skill.titleAr.lowercase())) score += 40
            skill.keywords.forEach { kw ->
                if (kw.isNotBlank() && lower.contains(kw.lowercase())) {
                    score += 18
                }
            }
            skill.packages.forEach { pkg ->
                if (pkg.isNotBlank() && lower.contains(pkg.lowercase())) {
                    score += 22
                }
            }
            skill.references.forEach { ref ->
                val cleanRef = ref.fileName.removeSuffix(".md").lowercase()
                if (cleanRef.length > 3 && lower.contains(cleanRef)) {
                    score += 15
                }
            }
            skill to score
        }

        val matched = scored
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxMatched)
            .map { it.first }

        return if (matched.isNotEmpty()) {
            matched
        } else {
            allSkills.filter { it.id in pinnedSkillIds }.take(maxMatched)
        }
    }

    /**
     * Builds a rich system prompt grounding block containing the matched skills, packages,
     * CLI commands, reference guidelines, and code snippets.
     */
    fun buildSkillsContextBlock(
        matchedSkills: List<ExtractedSkillItem>,
        mcpServerUrl: String = "https://mcp.expo.dev/mcp"
    ): String {
        if (matchedSkills.isEmpty()) return ""
        return buildString {
            appendLine("=== [ACTIVE SKILLS & TOOLS MEMORY BANK — AUTO-INJECTED] ===")
            if (mcpServerUrl.isNotBlank()) {
                appendLine("MCP endpoint listed in metadata only; this app does not establish a live MCP connection: $mcpServerUrl")
            }
            appendLine("Active Skills Applied (${matchedSkills.size}): ${matchedSkills.joinToString(", ") { "${it.id} (${it.titleAr})" }}")
            appendLine()
            matchedSkills.forEach { skill ->
                appendLine("--- SKILL: ${skill.id} | ${skill.titleAr} [${skill.category}] ---")
                appendLine("Description: ${skill.descriptionEn}")
                if (skill.packages.isNotEmpty()) {
                    appendLine("Recommended Packages/Libraries: ${skill.packages.joinToString(", ")}")
                }
                if (skill.cliCommands.isNotEmpty()) {
                    appendLine("Key CLI Commands: ${skill.cliCommands.take(5).joinToString(" | ")}")
                }
                appendLine("Core Instructions Summary:")
                appendLine(skill.skillMarkdown.take(1800))
                if (skill.references.isNotEmpty()) {
                    appendLine("Sub-References Available (${skill.references.size}): ${skill.references.joinToString(", ") { it.fileName }}")
                    val topRef = skill.references.first()
                    appendLine("Reference Excerpt (${topRef.fileName}):")
                    appendLine(topRef.content.take(900))
                }
                appendLine()
            }
            appendLine("=== [END OF ACTIVE SKILLS MEMORY BANK] ===")
        }
    }

    /**
     * Serializes the user's pinned skills and memory state into a Firestore-ready JSON payload
     * for `/users/{userId}/collections` (`DbCollectionSchema`).
     */
    fun buildFirestoreRegistryJson(
        pinnedIds: Set<String>,
        catalog: SkillsMemoryCatalog
    ): String {
        val array = JSONArray()
        catalog.skills.forEach { skill ->
            val obj = JSONObject()
            obj.put("id", skill.id)
            obj.put("titleAr", skill.titleAr)
            obj.put("category", skill.category)
            obj.put("isPinned", skill.id in pinnedIds)
            obj.put("referencesCount", skill.references.size)
            obj.put("scriptsCount", skill.scripts.size)
            obj.put("packagesCount", skill.packages.size)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseCatalogJson(jsonText: String): SkillsMemoryCatalog {
        val root = JSONObject(jsonText)
        val skillsArr = root.optJSONArray("skills") ?: JSONArray()
        val skillsList = ArrayList<ExtractedSkillItem>(skillsArr.length())

        for (i in 0 until skillsArr.length()) {
            val s = skillsArr.getJSONObject(i)
            val kwArr = s.optJSONArray("keywords") ?: JSONArray()
            val keywords = (0 until kwArr.length()).map { kwArr.getString(it) }

            val pkgArr = s.optJSONArray("packages") ?: JSONArray()
            val packages = (0 until pkgArr.length()).map { pkgArr.getString(it) }

            val cmdArr = s.optJSONArray("cliCommands") ?: JSONArray()
            val commands = (0 until cmdArr.length()).map { cmdArr.getString(it) }

            val snipArr = s.optJSONArray("codeSnippets") ?: JSONArray()
            val snippets = (0 until snipArr.length()).map { idx ->
                val sn = snipArr.getJSONObject(idx)
                SkillCodeSnippet(
                    language = sn.optString("language", "ts"),
                    code = sn.optString("code", "")
                )
            }

            val refArr = s.optJSONArray("references") ?: JSONArray()
            val references = (0 until refArr.length()).map { idx ->
                val rf = refArr.getJSONObject(idx)
                val rfSnipArr = rf.optJSONArray("codeBlocks") ?: JSONArray()
                val rfSnips = (0 until rfSnipArr.length()).map { bIdx ->
                    val bObj = rfSnipArr.getJSONObject(bIdx)
                    SkillCodeSnippet(
                        language = bObj.optString("language", "ts"),
                        code = bObj.optString("code", "")
                    )
                }
                SkillReferenceDoc(
                    fileName = rf.optString("fileName", ""),
                    path = rf.optString("path", ""),
                    content = rf.optString("content", ""),
                    codeBlocks = rfSnips
                )
            }

            val scrArr = s.optJSONArray("scripts") ?: JSONArray()
            val scripts = (0 until scrArr.length()).map { idx ->
                val sc = scrArr.getJSONObject(idx)
                SkillScriptTool(
                    fileName = sc.optString("fileName", ""),
                    path = sc.optString("path", ""),
                    content = sc.optString("content", "")
                )
            }

            skillsList.add(
                ExtractedSkillItem(
                    id = s.optString("id", ""),
                    titleAr = s.optString("titleAr", ""),
                    category = s.optString("category", ""),
                    archivePath = s.optString("archivePath", ""),
                    descriptionEn = s.optString("descriptionEn", ""),
                    keywords = keywords,
                    skillMarkdown = s.optString("skillMarkdown", ""),
                    packages = packages,
                    cliCommands = commands,
                    codeSnippets = snippets,
                    references = references,
                    scripts = scripts,
                    isPinnedByDefault = s.optBoolean("isPinnedByDefault", false)
                )
            )
        }

        val toolsArr = root.optJSONArray("globalToolsAndConfigs") ?: JSONArray()
        val toolsList = (0 until toolsArr.length()).map { idx ->
            val t = toolsArr.getJSONObject(idx)
            GlobalToolConfigItem(
                name = t.optString("name", ""),
                path = t.optString("path", ""),
                type = t.optString("type", "TOOL_SCRIPT"),
                content = t.optString("content", "")
            )
        }

        val pathsArr = root.optJSONArray("allFilePaths") ?: JSONArray()
        val allPaths = (0 until pathsArr.length()).map { pathsArr.getString(it) }

        return SkillsMemoryCatalog(
            archiveName = root.optString("archiveName", "skills-main_b7d3bd06.zip"),
            totalArchiveFiles = root.optInt("totalArchiveFiles", allPaths.size),
            totalSkills = root.optInt("totalSkills", skillsList.size),
            mcpServerUrl = root.optString("mcpServerUrl", "https://mcp.expo.dev/mcp"),
            skills = skillsList,
            globalToolsAndConfigs = toolsList,
            allFilePaths = allPaths
        )
    }
}
