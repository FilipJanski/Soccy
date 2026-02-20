package com.example.soccy.ui.viewer

import java.io.File
import kotlin.math.abs

object PlyObjParser {

    /** Zwraca FloatArray: x,y,z,x,y,z... (punkty). */
    fun loadPoints(path: String): FloatArray {
        val f = File(path)
        if (!f.exists()) return floatArrayOf()

        return when {
            path.endsWith(".ply", ignoreCase = true) -> parseAsciiPly(f)
            path.endsWith(".obj", ignoreCase = true) -> parseObjVertices(f)
            else -> floatArrayOf()
        }
    }


    private fun parseAsciiPly(file: File): FloatArray {
        val lines = file.readLines()
        var headerEnd = -1
        var vertexCount = 0

        for (i in lines.indices) {
            val l = lines[i].trim()
            if (l.startsWith("element vertex ")) {
                vertexCount = l.removePrefix("element vertex ").trim().toIntOrNull() ?: 0
            }
            if (l == "end_header") {
                headerEnd = i
                break
            }
        }
        if (headerEnd == -1 || vertexCount <= 0) return floatArrayOf()

        val pts = ArrayList<Float>(vertexCount * 3)
        val start = headerEnd + 1
        val end = (start + vertexCount).coerceAtMost(lines.size)

        for (i in start until end) {
            val parts = lines[i].trim().split(Regex("\\s+"))
            if (parts.size < 3) continue
            val x = parts[0].toFloatOrNull() ?: continue
            val y = parts[1].toFloatOrNull() ?: continue
            val z = parts[2].toFloatOrNull() ?: continue
            pts.add(x); pts.add(y); pts.add(z)
        }


        return normalize(pts.toFloatArray())
    }

    private fun parseObjVertices(file: File): FloatArray {
        val pts = ArrayList<Float>(10000)
        file.forEachLine { line ->
            val l = line.trim()
            if (l.startsWith("v ")) {
                val parts = l.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val x = parts[1].toFloatOrNull()
                    val y = parts[2].toFloatOrNull()
                    val z = parts[3].toFloatOrNull()
                    if (x != null && y != null && z != null) {
                        pts.add(x); pts.add(y); pts.add(z)
                    }
                }
            }
        }
        return normalize(pts.toFloatArray())
    }

    private fun normalize(p: FloatArray): FloatArray {
        if (p.isEmpty()) return p
        var maxAbs = 0f
        for (v in p) maxAbs = maxAbs.coerceAtLeast(abs(v))
        if (maxAbs == 0f) return p
        val s = 1f / maxAbs
        return FloatArray(p.size) { i -> p[i] * s }
    }
}