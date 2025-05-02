package com.albertsons.acupick.ui.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hadilq.liveevent.LiveEvent

fun <T> MutableLiveData<T>.forceRefresh() {
    this.postValue(this.value)
}

fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.postValue(block(it, liveData.value))
    }
    result.addSource(liveData) {
        result.postValue(block(this.value, it))
    }
    return result
}

fun <T, K, S, R> LiveData<T>.triple(
    second: LiveData<K>,
    third: LiveData<S>,
    block: (T?, K?, S?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.postValue(block(it, second.value, third.value))
    }
    result.addSource(second) {
        result.postValue(block(this.value, it, third.value))
    }
    result.addSource(third) {
        result.postValue(block(this.value, second.value, it))
    }
    return result
}

fun <T, K, S, U, R> LiveData<T>.quadruple(
    second: LiveData<K>,
    third: LiveData<S>,
    fourth: LiveData<U>,
    block: (T?, K?, S?, U?) -> R,
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.postValue(block(it, second.value, third.value, fourth.value))
    }
    result.addSource(second) {
        result.postValue(block(this.value, it, third.value, fourth.value))
    }
    result.addSource(third) {
        result.postValue(block(this.value, second.value, it, fourth.value))
    }
    result.addSource(fourth) {
        result.postValue(block(this.value, second.value, third.value, it))
    }
    return result
}

fun <T, R> LiveData<T>.transform(block: (T?) -> R): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.postValue(block(it))
    }
    return result
}

fun <T, R> LiveData<T>.waitingTransform(block: (T?, MediatorLiveData<R>) -> Unit): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        block.invoke(it, result)
    }
    return result
}

fun LiveEvent<Unit>.post() {
    this.postValue(Unit)
}

fun <T> LiveData<T>.observeNonNullOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(
        lifecycleOwner,
        object : Observer<T> {
            override fun onChanged(t: T) {
                t?.let {
                    observer.onChanged(t)
                    removeObserver(this)
                }
            }
        }
    )
}
