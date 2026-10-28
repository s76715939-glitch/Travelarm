package com.sstechzone.travelarm.data.model

enum class MapType(
    val titleEn: String,
    val titleBn: String,
    val descriptionEn: String,
    val descriptionBn: String
) {
    STREET(
        titleEn = "Street",
        titleBn = "রাস্তা",
        descriptionEn = "Standard road map with clear navigation paths",
        descriptionBn = "পরিষ্কার রাস্তা ও হাইওয়ে ভিউ"
    ),
    TOPOGRAPHIC(
        titleEn = "Topographic",
        titleBn = "টপোগ্রাফিক",
        descriptionEn = "Terrain elevation contours & outdoor details",
        descriptionBn = "পাহাড় ও প্রাকৃতিক ভূমিরূপের বিবরণ"
    ),
    SATELLITE(
        titleEn = "Satellite",
        titleBn = "স্যাটেলাইট",
        descriptionEn = "High-resolution global aerial imagery",
        descriptionBn = "বাস্তব উপগ্রহের হাই-রেজোলিউশন ছবি"
    );

    fun getTitle(language: AppLanguage): String {
        return if (language == AppLanguage.BANGLA) titleBn else titleEn
    }

    fun getDescription(language: AppLanguage): String {
        return if (language == AppLanguage.BANGLA) descriptionBn else descriptionEn
    }
}
