package com.example.myradio.data.local

import com.example.myradio.data.model.RadioStation

object StationsDataSource {

    fun getStations(): List<RadioStation> = listOf(
        RadioStation(
            id = 1,
            name = "SWR3",
            streamUrl = "https://liveradio.swr.de/sw282p3/swr3/play.mp3",
            genre = "Pop",
            country = "DE",
            logoUrl = "https://swr3.de/assets/swr3/icons/apple-touch-icon.png"
        ),
        RadioStation(
            id = 2,
            name = "SWR1 Baden-Württemberg",
            streamUrl = "https://liveradio.swr.de/sw282p3/swr1bw/play.mp3",
            genre = "Oldies / Pop",
            country = "DE",
            logoUrl = "https://www.swr.de/assets/swr/apple-touch-icon.png"
        ),
        RadioStation(
            id = 3,
            name = "Deutschlandfunk",
            streamUrl = "https://st01.dlf.de/dlf/01/128/mp3/stream.mp3",
            genre = "News / Kultur",
            country = "DE",
            logoUrl = "https://www.deutschlandfunk.de/static/img/deutschlandfunk/icons/apple-touch-icon-128x128.png"
        ),
        RadioStation(
            id = 4,
            name = "Deutschlandfunk Kultur",
            streamUrl = "https://st02.dlf.de/dlf/02/128/mp3/stream.mp3",
            genre = "Kultur",
            country = "DE",
            logoUrl = "https://www.deutschlandfunkkultur.de/static/img/deutschlandfunk_kultur/icons/apple-touch-icon-128x128.png"
        ),
        RadioStation(
            id = 5,
            name = "SRF 3",
            streamUrl = "https://stream.srg-ssr.ch/m/drs3/mp3_128",
            genre = "Pop / Rock",
            country = "CH",
            logoUrl = "https://www.srf.ch/static-assets/images/srf-apple-touch-icon.png"
        ),
        RadioStation(
            id = 6,
            name = "Radio Swiss Jazz",
            streamUrl = "https://stream.srg-ssr.ch/m/rsj/mp3_128",
            genre = "Jazz",
            country = "CH",
            logoUrl = "https://www.radioswissjazz.ch/favicon.ico"
        ),
        RadioStation(
            id = 7,
            name = "Radio Swiss Classic",
            streamUrl = "https://stream.srg-ssr.ch/m/rsc_de/mp3_128",
            genre = "Klassik",
            country = "CH",
            logoUrl = "https://www.radioswissclassic.ch/_nuxt/img/rsc-logo-claim-desktop.520c5d8.png"
        ),
        RadioStation(
            id = 8,
            name = "BBC Radio 1",
            streamUrl = "http://stream.live.vc.bbcmedia.co.uk/bbc_radio_one",
            genre = "Pop / Chart",
            country = "UK",
            logoUrl = "https://cdn-radiotime-logos.tunein.com/s24939q.png"
        ),
        RadioStation(
            id = 9,
            name = "SRF 1 (Aargau/Solothurn)",
            streamUrl = "https://stream.srg-ssr.ch/m/regi_ag_so/mp3_128",
            genre = "Regional / Info",
            country = "CH",
            logoUrl = "https://www.srf.ch/static-assets/images/srf-apple-touch-icon.png"
        ),
        RadioStation(
            id = 10,
            name = "Flashback.fm",
            streamUrl = "https://stream.streambase.ch/ffm/mp3-128",
            genre = "Oldies / Retro",
            country = "CH",
            logoUrl = "https://www.google.com/s2/favicons?domain=flashbackfm.ch&sz=128"
        ),
        RadioStation(
            id = 11,
            name = "Vintage Radio",
            streamUrl = "https://vintageradio.ice.infomaniak.ch/vintageradio-high.mp3",
            genre = "Oldies / Vintage",
            country = "CH",
            logoUrl = "https://www.google.com/s2/favicons?domain=vintageradio.ch&sz=128"
        ),
        RadioStation(
            id = 12,
            name = "Oldie-Antenne",
            streamUrl = "https://stream.oldie-antenne.de/oldie-antenne/stream/mp3",
            genre = "Oldies",
            country = "DE",
            logoUrl = "https://www.oldie-antenne.de/logos/oldie-antenne/apple-touch-icon.png"
        ),
        RadioStation(
            id = 13,
            name = "Harmony FM",
            streamUrl = "http://mp3.harmonyfm.de/harmonyfm/hqlivestream.mp3",
            genre = "80er Jahre",
            country = "DE",
            logoUrl = "https://www.google.com/s2/favicons?domain=harmonyfm.de&sz=128"
        ),
        RadioStation(
            id = 14,
            name = "SWR1 Rheinland-Pfalz",
            streamUrl = "https://liveradio.swr.de/sw282p3/swr1rp/play.mp3",
            genre = "Oldies / Pop",
            country = "DE",
            logoUrl = "https://www.swr.de/assets/swr/apple-touch-icon.png"
        ),
        RadioStation(
            id = 15,
            name = "DIE NEUE 107.7",
            streamUrl = "http://dieneue1077.cast.addradio.de/dieneue1077/simulcast/high/stream.mp3",
            genre = "Pop / Rock",
            country = "DE",
            logoUrl = "https://upload.dieneue1077.de/production/static/1713539298713/icons/icon_64.ax1w460g800.png"
        ),
        RadioStation(
            id = 16,
            name = "Antenne Soft Rock",
            streamUrl = "https://stream.antenne.de/soft-rock/stream/mp3",
            genre = "Soft Rock",
            country = "DE",
            logoUrl = "https://www.google.com/s2/favicons?domain=antenne.de&sz=128"
        ),
    )
}
