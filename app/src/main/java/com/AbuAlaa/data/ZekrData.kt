package com.AbuAlaa.data

import com.AbuAlaa.R

data class Zekr(val name: String, val resId: Int)

object ZekrData {
    val zekrList = listOf(
        Zekr("سبحان الله وبحمده", R.raw.sobhanallah_wabehamdeh),
        Zekr("الحمد لله", R.raw.alhamdo_lelah),
        Zekr("اللهم لك الحمد", R.raw.allahom_lk_alhamd),
        Zekr("لا حول ولا قوة إلا بالله", R.raw.lahawla_wlaqowat),
        Zekr("ربنا اغفر لي", R.raw.rbna_ighfer_li),
        Zekr("اللهم صل على الحبيب", R.raw.nozaker_salt_ala_habib),
        Zekr("آية الأحزاب", R.raw.ayah_elahzab)
    )

    val sobhanallah_wabehemden = "سبحان الله وبحمده"
}
