package com.learnopengles.android.common

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object RawResourceReader {
    fun readTextFileFromRawResource(context: Context,
                                    resourceId: Int): String? {
        val inputStream = context.resources.openRawResource(resourceId)
        val inputStreamReader = InputStreamReader(inputStream)
        val bufferedReader = BufferedReader(inputStreamReader)

        var nextLine: String?
        val body = StringBuilder()

        try {
            nextLine = bufferedReader.readLine()
            while (nextLine != null) {
                body.append(nextLine)
                body.append('\n')
                nextLine = bufferedReader.readLine()
            }
        } catch (e: IOException) {
            return null
        }

        return body.toString()
    }
}
