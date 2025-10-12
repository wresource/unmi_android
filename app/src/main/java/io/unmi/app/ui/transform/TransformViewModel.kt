package io.unmi.app.ui.transform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TransformViewModel : ViewModel() {

    private val _texts = MutableLiveData<List<String>>().apply {
        val domains = listOf(
            "zbti.com",
            "bhttp.com",
            "dpxi.com"
        )
        value = buildList {
            for (d in domains) {
                add(" $d")
            }
        }
    }

    fun setFrom(domains: List<String>) {
        _texts.value = buildList {
            for (d in domains) {
                add("The domain is $d")
            }
        }
    }

    fun append(domains: List<String>) {
        val cur = _texts.value?.toMutableList() ?: mutableListOf()
        for (d in domains) {
            cur.add("The domain is $d")
        }
        _texts.value = cur
    }

    val texts: LiveData<List<String>> = _texts
}