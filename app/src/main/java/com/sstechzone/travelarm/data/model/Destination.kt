package com.sstechzone.travelarm.data.model

data class Destination(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val addressBn: String,
    val addressEn: String,
    val latitude: Double,
    val longitude: Double,
    val category: DestinationCategory = DestinationCategory.TRANSIT_HUB
)

enum class DestinationCategory {
    TRANSIT_HUB,
    BUS_TERMINAL,
    RAILWAY_STATION,
    LAUNCH_TERMINAL,
    AIRPORT,
    CITY_CENTER,
    CUSTOM
}

object BangladeshTransitHubs {
    val hubs = listOf(
        // Dhaka Major Terminals & Interchanges
        Destination(
            id = "dhaka_gabtoli",
            nameBn = "গাবতলী বাস টার্মিনাল",
            nameEn = "Gabtoli Bus Terminal",
            addressBn = "মিরপুর রোড, ঢাকা",
            addressEn = "Mirpur Road, Dhaka",
            latitude = 23.7779,
            longitude = 90.3475,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "dhaka_mohakhali",
            nameBn = "মহাখালী বাস টার্মিনাল",
            nameEn = "Mohakhali Bus Terminal",
            addressBn = "বীর উত্তম এ কে খন্দকার রোড, ঢাকা",
            addressEn = "Bir Uttam AK Khandakar Rd, Dhaka",
            latitude = 23.7788,
            longitude = 90.4005,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "dhaka_sayedabad",
            nameBn = "সায়েদাবাদ বাস টার্মিনাল",
            nameEn = "Sayedabad Bus Terminal",
            addressBn = "যাত্রাবাড়ী সংযোগ সড়ক, ঢাকা",
            addressEn = "Jatrabari Link Rd, Dhaka",
            latitude = 23.7153,
            longitude = 90.4285,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "dhaka_kamalapur",
            nameBn = "কমলাপুর রেলওয়ে স্টেশন",
            nameEn = "Kamalapur Railway Station",
            addressBn = "মতিঝিল, ঢাকা",
            addressEn = "Motijheel, Dhaka",
            latitude = 23.7317,
            longitude = 90.4255,
            category = DestinationCategory.RAILWAY_STATION
        ),
        Destination(
            id = "dhaka_airport_railway",
            nameBn = "বিমানবন্দর রেলওয়ে স্টেশন",
            nameEn = "Airport Railway Station",
            addressBn = "উত্তরা, ঢাকা",
            addressEn = "Uttara, Dhaka",
            latitude = 23.8519,
            longitude = 90.4079,
            category = DestinationCategory.RAILWAY_STATION
        ),
        Destination(
            id = "dhaka_sadarghat",
            nameBn = "সদরঘাট লঞ্চ টার্মিনাল",
            nameEn = "Sadarghat Launch Terminal",
            addressBn = "বুড়িগঙ্গা নদীর তীর, পুরান ঢাকা",
            addressEn = "Buriganga Riverfront, Old Dhaka",
            latitude = 23.7028,
            longitude = 90.4132,
            category = DestinationCategory.LAUNCH_TERMINAL
        ),
        // Dhaka Prominent Areas & Bus Stops
        Destination(
            id = "dhaka_farmgate",
            nameBn = "ফার্মগেট বাস স্টপ",
            nameEn = "Farmgate Bus Stop",
            addressBn = "ফার্মগেট, তেজগাঁও, ঢাকা",
            addressEn = "Farmgate, Tejgaon, Dhaka",
            latitude = 23.7561,
            longitude = 90.3872,
            category = DestinationCategory.TRANSIT_HUB
        ),
        Destination(
            id = "dhaka_shahbagh",
            nameBn = "শাহবাগ মোড়",
            nameEn = "Shahbagh Mor",
            addressBn = "শাহবাগ, ঢাকা বিশ্ববিদ্যালয় এলাকা",
            addressEn = "Shahbagh, Dhaka University Area",
            latitude = 23.7383,
            longitude = 90.3957,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "dhaka_dhanmondi_27",
            nameBn = "ধানমন্ডি ২৭ / সোবহানবাগ",
            nameEn = "Dhanmondi 27 / Sobhanbag",
            addressBn = "মিরপুর রোড, ধানমন্ডি, ঢাকা",
            addressEn = "Mirpur Road, Dhanmondi, Dhaka",
            latitude = 23.7533,
            longitude = 90.3769,
            category = DestinationCategory.TRANSIT_HUB
        ),
        Destination(
            id = "dhaka_mirpur_10",
            nameBn = "মিরপুর ১০ গোলচত্বর",
            nameEn = "Mirpur 10 Circle",
            addressBn = "মিরপুর, ঢাকা",
            addressEn = "Mirpur, Dhaka",
            latitude = 23.8069,
            longitude = 90.3687,
            category = DestinationCategory.TRANSIT_HUB
        ),
        Destination(
            id = "dhaka_uttara_jasimuddin",
            nameBn = "উত্তরা জসীমউদ্দীন মোড়",
            nameEn = "Uttara Jasimuddin Mor",
            addressBn = "ঢাকা-ময়মনসিংহ রোড, সেক্টর ৩, উত্তরা",
            addressEn = "Dhaka-Mymensingh Rd, Sector 3, Uttara",
            latitude = 23.8617,
            longitude = 90.3995,
            category = DestinationCategory.TRANSIT_HUB
        ),
        Destination(
            id = "dhaka_gulshan_2",
            nameBn = "গুলশান ২ গোলচত্বর",
            nameEn = "Gulshan 2 Circle",
            addressBn = "গুলশান, ঢাকা",
            addressEn = "Gulshan, Dhaka",
            latitude = 23.7946,
            longitude = 90.4143,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "dhaka_motijheel",
            nameBn = "মতিঝিল শাপলা চত্বর",
            nameEn = "Motijheel Shapla Chottor",
            addressBn = "মতিঝিল বাণিজ্যিক এলাকা, ঢাকা",
            addressEn = "Motijheel Commercial Area, Dhaka",
            latitude = 23.7289,
            longitude = 90.4193,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "dhaka_kuril_bishwa_road",
            nameBn = "কুড়িল বিশ্বরোড ফ্লাইওভার",
            nameEn = "Kuril Bishwa Road Flyover",
            addressBn = "প্রগতি সরণি সংলগ্ন, কুড়িল, ঢাকা",
            addressEn = "Kuril, Pragati Sarani, Dhaka",
            latitude = 23.8182,
            longitude = 90.4208,
            category = DestinationCategory.TRANSIT_HUB
        ),
        Destination(
            id = "dhaka_gazipur_chowrasta",
            nameBn = "গাজীপুর চৌরাস্তা",
            nameEn = "Gazipur Chowrasta",
            addressBn = "ঢাকা-ময়মনসিংহ ও টাঙ্গাইল সংযোগ মোড়, গাজীপুর",
            addressEn = "Gazipur Chowrasta Interchange",
            latitude = 23.9984,
            longitude = 90.3842,
            category = DestinationCategory.TRANSIT_HUB
        ),
        Destination(
            id = "dhaka_savar_bus_stand",
            nameBn = "সাভার বাস স্ট্যান্ড",
            nameEn = "Savar Bus Stand",
            addressBn = "ঢাকা-আরিচা মহাসড়ক, সাভার",
            addressEn = "Dhaka-Aricha Hwy, Savar",
            latitude = 23.8441,
            longitude = 90.2584,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "dhaka_narayanganj_chasara",
            nameBn = "চাষাড়া মোড়, নারায়ণগঞ্জ",
            nameEn = "Chashara Mor, Narayanganj",
            addressBn = "বঙ্গবন্ধু সড়ক, নারায়ণগঞ্জ",
            addressEn = "Bangabandhu Rd, Narayanganj",
            latitude = 23.6238,
            longitude = 90.4990,
            category = DestinationCategory.TRANSIT_HUB
        ),
        // Chittagong & South-East
        Destination(
            id = "ctg_brtc",
            nameBn = "চট্টগ্রাম বিআরটিসি বাস ডিপো",
            nameEn = "Chattogram BRTC Bus Depot",
            addressBn = "স্টেশন রোড, চট্টগ্রাম",
            addressEn = "Station Road, Chattogram",
            latitude = 22.3364,
            longitude = 91.8315,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "ctg_railway",
            nameBn = "চট্টগ্রাম রেলওয়ে স্টেশন",
            nameEn = "Chattogram Railway Station",
            addressBn = "নিউ মার্কেট এলাকা, চট্টগ্রাম",
            addressEn = "New Market Area, Chattogram",
            latitude = 22.3353,
            longitude = 91.8291,
            category = DestinationCategory.RAILWAY_STATION
        ),
        Destination(
            id = "ctg_gec_circle",
            nameBn = "জিইসি মোড়, চট্টগ্রাম",
            nameEn = "GEC Circle, Chattogram",
            addressBn = "নাসিরাবাদ, চট্টগ্রাম",
            addressEn = "Nasirabad, Chattogram",
            latitude = 22.3592,
            longitude = 91.8216,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "ctg_agrabad",
            nameBn = "আগ্রাবাদ বাণিজ্যিক এলাকা",
            nameEn = "Agrabad Commercial Area",
            addressBn = "আগ্রাবাদ এক্সেস রোড, চট্টগ্রাম",
            addressEn = "Agrabad Access Rd, Chattogram",
            latitude = 22.3255,
            longitude = 91.8130,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "ctg_bahaddarhat",
            nameBn = "বহদ্দারহাট বাস টার্মিনাল",
            nameEn = "Bahaddarhat Bus Terminal",
            addressBn = "বহদ্দারহাট, চট্টগ্রাম",
            addressEn = "Bahaddarhat, Chattogram",
            latitude = 22.3703,
            longitude = 91.8491,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "cox_bazar_dolphin",
            nameBn = "ডলফিন মোড় / সুগন্ধা পয়েন্ট",
            nameEn = "Dolphin Mor / Sugandha Point",
            addressBn = "কলাতলী রোড, কক্সবাজার",
            addressEn = "Kolatoli Road, Cox's Bazar",
            latitude = 21.4272,
            longitude = 91.9818,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "cox_bazar_bus_terminal",
            nameBn = "কক্সবাজার কেন্দ্রীয় বাস টার্মিনাল",
            nameEn = "Cox's Bazar Central Bus Terminal",
            addressBn = "ঝিলংজা, কক্সবাজার",
            addressEn = "Jhilongja, Cox's Bazar",
            latitude = 21.4428,
            longitude = 91.9892,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "cumilla_paduar_bazar",
            nameBn = "পদুয়ার বাজার বিশ্বরোড",
            nameEn = "Paduar Bazar Bishwa Road",
            addressBn = "ঢাকা-চট্টগ্রাম হাইওয়ে, কুমিল্লা",
            addressEn = "Dhaka-Ctg Hwy, Cumilla",
            latitude = 23.4243,
            longitude = 91.1738,
            category = DestinationCategory.TRANSIT_HUB
        ),
        Destination(
            id = "cumilla_kandirpar",
            nameBn = "কান্দিরপাড়, কুমিল্লা",
            nameEn = "Kandirpar, Cumilla",
            addressBn = "কুমিল্লা সদর",
            addressEn = "Cumilla Sadar",
            latitude = 23.4613,
            longitude = 91.1809,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "feni_mohipal",
            nameBn = "মহিপাল ফ্লাইওভার / বাস স্ট্যান্ড",
            nameEn = "Mohipal Bus Stand, Feni",
            addressBn = "ঢাকা-চট্টগ্রাম হাইওয়ে, ফেনী",
            addressEn = "Dhaka-Ctg Hwy, Feni",
            latitude = 23.0186,
            longitude = 91.3912,
            category = DestinationCategory.BUS_TERMINAL
        ),
        // Sylhet Division
        Destination(
            id = "sylhet_kadamtali",
            nameBn = "কদমতলী বাস টার্মিনাল, সিলেট",
            nameEn = "Kadamtali Bus Terminal, Sylhet",
            addressBn = "দক্ষিণ সুরমা, সিলেট",
            addressEn = "South Surma, Sylhet",
            latitude = 24.8841,
            longitude = 91.8679,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "sylhet_railway",
            nameBn = "সিলেট রেলওয়ে স্টেশন",
            nameEn = "Sylhet Railway Station",
            addressBn = "কদমতলী, সিলেট",
            addressEn = "Kadamtali, Sylhet",
            latitude = 24.8808,
            longitude = 91.8690,
            category = DestinationCategory.RAILWAY_STATION
        ),
        Destination(
            id = "sylhet_zindabazar",
            nameBn = "জিন্দাবাজার পয়েন্ট, সিলেট",
            nameEn = "Zindabazar Point, Sylhet",
            addressBn = "জিন্দাবাজার, সিলেট",
            addressEn = "Zindabazar, Sylhet",
            latitude = 24.8967,
            longitude = 91.8706,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "sreemangal_bus_stand",
            nameBn = "শ্রীমঙ্গল বাস স্ট্যান্ড",
            nameEn = "Sreemangal Bus Stand",
            addressBn = "মৌলভীবাজার রোড, শ্রীমঙ্গল",
            addressEn = "Moulvibazar Rd, Sreemangal",
            latitude = 24.3075,
            longitude = 91.7312,
            category = DestinationCategory.BUS_TERMINAL
        ),
        // Rajshahi & Rangpur
        Destination(
            id = "rajshahi_shirmoil",
            nameBn = "শিরোইল বাস টার্মিনাল, রাজশাহী",
            nameEn = "Shiroil Bus Terminal, Rajshahi",
            addressBn = "রেলগেট সংলগ্ন, রাজশাহী",
            addressEn = "Railgate, Rajshahi",
            latitude = 24.3745,
            longitude = 88.6042,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "rajshahi_railway",
            nameBn = "রাজশাহী রেলওয়ে স্টেশন",
            nameEn = "Rajshahi Railway Station",
            addressBn = "শিরোইল, রাজশাহী",
            addressEn = "Shiroil, Rajshahi",
            latitude = 24.3740,
            longitude = 88.6045,
            category = DestinationCategory.RAILWAY_STATION
        ),
        Destination(
            id = "bogura_satmatha",
            nameBn = "সাতমাথা মোড়, বগুড়া",
            nameEn = "Satmatha Mor, Bogura",
            addressBn = "বগুড়া শহর কেন্দ্র",
            addressEn = "Bogura City Center",
            latitude = 24.8510,
            longitude = 89.3732,
            category = DestinationCategory.CITY_CENTER
        ),
        Destination(
            id = "rangpur_kamarpur",
            nameBn = "কামারপাড়া ঢাকা কোচ স্ট্যান্ড, রংপুর",
            nameEn = "Kamarpura Bus Stand, Rangpur",
            addressBn = "রংপুর সদর",
            addressEn = "Rangpur Sadar",
            latitude = 25.7538,
            longitude = 89.2458,
            category = DestinationCategory.BUS_TERMINAL
        ),
        // Khulna & Barisal
        Destination(
            id = "khulna_sonadanga",
            nameBn = "সোনাডাঙ্গা বাস টার্মিনাল, খুলনা",
            nameEn = "Sonadanga Bus Terminal, Khulna",
            addressBn = "মজিদ সরণি, সোনাডাঙ্গা, খুলনা",
            addressEn = "Majid Sarani, Sonadanga, Khulna",
            latitude = 22.8252,
            longitude = 89.5401,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "khulna_railway",
            nameBn = "খুলনা রেলওয়ে স্টেশন",
            nameEn = "Khulna Railway Station",
            addressBn = "স্টেশন রোড, খুলনা",
            addressEn = "Station Road, Khulna",
            latitude = 22.8138,
            longitude = 89.5636,
            category = DestinationCategory.RAILWAY_STATION
        ),
        Destination(
            id = "barisal_nathullabad",
            nameBn = "নথুল্লাবাদ কেন্দ্রীয় বাস টার্মিনাল",
            nameEn = "Nathullabad Central Bus Terminal",
            addressBn = "কাশীপুর, বরিশাল",
            addressEn = "Kashipur, Barishal",
            latitude = 22.7161,
            longitude = 90.3547,
            category = DestinationCategory.BUS_TERMINAL
        ),
        Destination(
            id = "barisal_launch_ghat",
            nameBn = "বরিশাল নদীবন্দর (লঞ্চ ঘাট)",
            nameEn = "Barishal Launch Ghat",
            addressBn = "কীর্তনখোলা নদীর তীর, বরিশাল",
            addressEn = "Kirtankhola Riverfront, Barishal",
            latitude = 22.7029,
            longitude = 90.3752,
            category = DestinationCategory.LAUNCH_TERMINAL
        ),
        Destination(
            id = "mymensingh_maskanda",
            nameBn = "মাসকান্দা বাস টার্মিনাল, ময়মনসিংহ",
            nameEn = "Maskanda Bus Terminal, Mymensingh",
            addressBn = "ঢাকা-ময়মনসিংহ মহাসড়ক, ময়মনসিংহ",
            addressEn = "Dhaka-Mymensingh Hwy, Mymensingh",
            latitude = 24.7471,
            longitude = 90.4072,
            category = DestinationCategory.BUS_TERMINAL
        )
    )
}
