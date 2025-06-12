package com.example.falldetectorapp.ml

import android.content.Context
import ai.onnxruntime.*
import java.nio.FloatBuffer

class RFClassifier(context: Context) {
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("model_rf.onnx").readBytes()
        session = ortEnv.createSession(modelBytes)
    }

    fun predict(features: FloatArray): Float {
        val inputShape = longArrayOf(1, features.size.toLong()) // 1 wiersz, n kolumn
        val inputBuffer = FloatBuffer.wrap(features)
        val tensor = OnnxTensor.createTensor(ortEnv, inputBuffer, inputShape)
//
//        val results = session.run(mapOf(session.inputNames.iterator().next() to tensor))
////        val output = results[0].value as Array<FloatArray>
////        return output[0][0] // zakładamy: 0 = brak upadku, 1 = upadek
//
//        val output = results[0].value
//        return when (output) {
//            is Array<FloatArray> -> output[0][0]
//            is LongArray -> output[0].toFloat()
//            is IntArray -> output[0].toFloat()
//            is FloatArray -> output[0]
//            else -> throw IllegalStateException("Nieobsługiwany typ wyjściowy modelu: ${output?.javaClass}")
//        }
        val results = session.run(mapOf(session.inputNames.iterator().next() to tensor))
        val output = results[0].value

        return when (output) {
            is LongArray -> output[0].toFloat()
            is IntArray -> output[0].toFloat()
            is FloatArray -> output[0]
            is Array<*> -> {
                val first = output.firstOrNull()
                if (first is FloatArray) {
                    return first[0]
                } else {
                    throw IllegalStateException("Nieznany typ wewnątrz Array: ${first?.javaClass}")
                }
            }
            else -> throw IllegalStateException("Nieobsługiwany typ wyjściowy modelu: ${output?.javaClass}")
        }


    }
}