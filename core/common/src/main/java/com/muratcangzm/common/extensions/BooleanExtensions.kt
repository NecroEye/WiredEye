package com.muratcangzm.common.extensions


import android.view.View

fun Boolean?.orFalse(): Boolean = this ?: false

fun Boolean?.orTrue(): Boolean = this ?: true

fun Boolean.toInt(): Int = if (this) 1 else 0

fun Boolean.toViewVisible(): Int = if (this) View.VISIBLE else View.GONE