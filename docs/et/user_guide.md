---
layout: page
comments: true
title: Kasutusjuhend
---

## Sissejuhatus

__Kõnele__ on kõnetuvastusteenus Androidi rakendustele, mis võimaldab
saata e-kirju, sooritada infootsingut, kirjutada märkmeid, anda käske jne kõne abil.

Kõnele kasutab kõne tekstiks teisendamiseks (ehk transkribeerimiseks) TTÜ Küberneetika Instituudi
foneetika ja kõnetehnoloogia laboris (vt <http://phon.ioc.ee>) välja töötatud
serveripõhist kõnetuvastustarkvara, mis on maailmas hetkel ainus, mis
sisendina eesti keelt toetab.

Kõnele kasutamiseks peab olema nutiseadmes internetiühendus sisse lülitatud.
Sõltuvalt mobiilioperaatori teenusepaketist võib interneti kasutamise hind
sõltuda andmemahtudest. Seega tasub teada, et pooleminutise kõne
transkribeerimiseks laaditakse serverisse umbes 1MB jagu andmeid. Wifivõrkudes
on Kõnele kasutuskiirus tüüpiliselt oluliselt parem kui 3G jms võrkudes.

Järgnev juhend kirjeldab Kõnele seadistamist ja kasutamist eestikeelse kasutajaliidesega
Android v5 (Lollipop) seadmes. Teistes Androidi seadmetes on menüüde nimed ja struktuur natuke
teistsugune, kuid mitte oluliselt.

(Selle juhendi eelmine versioon on [siin](https://code.google.com/p/recognizer-intent/wiki/UserGuide?wl=et).)

## Demo

Järgnev video näitab
(1) kõnepõhist veebiotsingut;
(2) kõneklaviatuuri sisse lülitamist ja sellega kirja kirjutamist;
(3) aadressiotsingut kaardirakenduses;
(4) Kõnele konfigureerimist Androidi vaikimisi kõnetuvastajaks, ja selle kasutamist _Google Translate_ rakenduses;
(5) tõlkegrammatika omistamist _Wolfram Alpha_ rakendusele, ja selles mõõtühikuteisendamist;
(6) _Arvutaja_ rakenduse kasutamist (vahelduseks inglise keeles) äratuskella seadmiseks.

<iframe id="ytplayer" type="text/html" width="480" height="360"
src="http://www.youtube.com/embed/gKFIWSA2GWc?origin=http://kaljurand.github.io"
frameborder="0"> </iframe>

Allpool tuleb kõikidest nendest kasutusolukordadest lähemalt juttu.

## Kõnepõhine veebiotsing

Vajutades Kõnele käivitusikoonile (_launcher icon_) avaneb punase nupuga paneel.
Nupule vajutades teisendab Kõnele sisendkõne tekstiks ning edastab selle
edasi seadme veebibrauserile.

<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-35-34.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-36-33.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-37-22.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-22-08-11.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-37-56.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-38-07.png">

Pärast Lindista-nupule vajutamist (seadetes saab ka määrata,
et lindistamine algab automaatselt, ilma nupule vajutamata),
tuleb oma jutt selge ja kõlava häälega mikrofoni rääkida.
Lindistamine lõpeb pärast pikemat pausi või nupule vajutamist (sõltuvalt
seadetest). Kui midagi läheb sassi, siis saab _Back_-nupule vajutades lindistamise
katkestada.
Lindistamise käigus näitab Kõnele mitu sekundit on lindistamine
juba kestnud ning mitu baiti on salvestus juba ruumi võtnud. Vastav kogus saadetakse
serverisse transkribeerimiseks.
Pärast lindistamise lõppu kuvatakse pilt lindistuse valjuse muutumisest
läbi lindistuse kestuse.
Selle pildi põhjal on võimalik otsustada, kas lindistus tehniliselt õnnestus või mitte.
Samal ajal toimub serveris kõne teisendamine tekstiks.
Tüüpiliselt tagastab server mitu tuvastushüpoteesi. Neist ühele vajutades käivitub
veebiotsing.

## Kõnele seadistamine

Sama punase nupuga paneeli ülemises paremas nurgas on nupp, mis viib Kõnele seadetesse.
Need seaded võimaldavad Kõnele töökäiku erinevatel viisidel suunata, määrates nt

- milliseid tuvastusteenusi ja -keeli kasutatakse;
- kas lindistamine algab automaatselt või peale nupule vajutamist;
- kas lindistamine lõpeb kui sisendkõnesse tekib paus, või alles siis, kui nupule on vajutatud;
- kas lindistamise algusest ja lõpust teavitatakse lühikese helisignaaliga.

Mõned nendest seadetest puudutavad ainult Kõnele enda kasutajaliidest ning
seega ei rakendu juhul kui Kõnelet kasutatakse läbi teise rakenduse.

<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-23-47-50.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-23-48-00.png">

Samuti on võimalik vaadata, läbi milliste rakenduste on kõnetuvastust kasutatud ning
omistada erinevatele rakendustele erinevaid grammatikaid (vt allpool).

Kõnele sisaldab kahte erinevat kõnetuvastusteenust, mis kasutavad vastavalt kahte erinevat
kõnetuvastusserverit:

  - "grammatikatoega" teenus (kasutab serverit tarkvaraga <http://github.com/alumae/ruby-pocketsphinx-server>)
    lubab sisendkõnele omistada tõlkegrammatikaid, kuid on aeglasem ja sisendkõne pikkus ei tohi ületada
    30 sekundit;
  - "kiire tuvastusega" teenus (kasutab serverit tarkvaraga <http://github.com/alumae/kaldi-gstreamer-server>)
    tagastab tuvastustulemuse juba rääkimise ajal, ega sea sisendkõne pikkusele mingit piirangut.

Mõlema teenuse poolt kasutatava serveri aadressi on võimalik seadetes muuta.

Lisaks Kõnele oma seadetele, on Kõnelet võimalik konfigureerida kolmes Androidi süsteemses
menüüs:

  - Androidi klaviatuuriseaded
  - Androidi kõnetuvastusteenuste seaded
  - Androidi kõnetuvastusrakenduste vaikeväärtused

Neist tuleb juttu allpool.

## Kõnele klaviatuurirakendusena

Paljudes Androidi rakendustes on tekstikastid, millele vajutades avaneb klaviatuurirakendus,
nn sisestusmeetod, inglise keeles "input method editor (IME)".
Kõnele sisaldab endas sellist klaviatuurirakendust, kuid erinevalt traditsioonilisest
tähtede ja numbritega klahvist on Kõnele klaviatuuril ainult üks nupp, mis
võimaldab kõnesisendit.

<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-14-00-15-41.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-14-00-23-30.png">

Kõnele klaviatuuri kasutamiseks tuleb see ennem sisse lülitada Androidi süsteemses menüüs
`Seaded -> Keeled ja sisestamine -> Klaviatuur ja sisestusmeetodid`, valides
seal Kõnele, ning muutes selle "praeguseks klaviatuuriks".

<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-32-57.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-33-10.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-33-51.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-34-00.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-34-24.png">

Lisaks nupule, mis käivitab/lõpetab/katkestab kõnetuvastuse, toetab Kõnele
klaviatuur järgmisi operatsioone:

- svaip vasakule kustutab kursorist vasakul asuva sõna,
- svaip paremale lisab reavahetuse,
- topeltvajutus lisab tühiku,
- vajutus klaviatuuriikoonile vahetab klaviatuuri,
- pikk vajutus klaviatuuriikoonile avab klaviatuurivahetusmenüü,
- vajutus otsinguikoonile käivitab otsingu (ainult otsingureal).

Kõneklaviatuuri on mõistlik kasutada paralleelselt mõne "tavaklaviatuuriga"
(nt _Google Keyboard_, _Swype_, _SwiftKey_, _SlideIT_).
Kõnetuvastuse abil tekstide dikteerimine sobib peamiselt olukordadesse,
kus keskkond on vaikne ja privaatne, ja tekst ei pea olema keeleliselt täiesti perfektne.
Sellistes olukordades on kõnetuvastuse kasutamine reeglina kiirem ja loomulikum
(ning lisaks võtab kõneklaviatuur ekraanil vähem ruumi).
Muudes olukordades võib ümber lülitada teisele klaviatuurile.
Android v5+ seadmetes on lihtne teksti kirjutamise ajal klaviatuure
vahetada, sest seadme põhinuppude (_Back_, _Home_, _Recent apps_) kõrvale ilmub teksti kirjutamise
ajaks klaviatuurivahetusnupp. Mõned klaviatuurid (nt _Google Keyboard_ ja _Kõnele_ ise)
võimaldavad lisaks klaviatuurivahetust ainult ühe nupuvajutusega. Nt, vajutades maakera-ikooni
_Google Keyboard_ klaviatuuril vahetub klaviatuur _Kõnele_ vastu; vajutades klaviatuuri-ikooni
_Kõnele_ klaviatuuril, vahetub klaviatuur tagasi _Google Keyboard_ klaviatuurile.
Selles rotatsioonis võib osaleda ka rohkem klaviatuure,
kui nad samamoodi vastavat Androidi klaviatuurivahetusliidest toetavad.

<img src="{{ site.baseurl }}/images/et/Screenshot_2015-05-11-21-44-34.png">&harr;<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-14-01-46-37.png">

Kõneklaviatuur kasutab vaikimisi "kiire tuvastusega" kõnetuvastusteenust, kuid
lisada saab ka teisi seadmesse installeeritud teenuseid ja nende poolt toetatud
keeli (nt Kõnele grammatika-teadlik teenus ja Google'i teenus),
muutes kõneklaviatuuri vastavat seadet Kõnele seadetes.
Kui valitud on mitu teenust/keelt, siis on klaviatuuril lisaks veel teenuse/keele vahetamise nupp:

- lühike vajus lülitab järgmisele teenusele/keelele,
- pikk vajutus kuvab kõik võimalikud teenused/keeled, võimaldades valikut muuta.

(Vt näidet peatükis "Grammatika-põhine kõnetuvastus".)


## Kõnele kutsumine teistest rakendustest (koos kasutajaliidesega)

Mõnes rakenduses (nt _Google Keep_) on mikrofoninupp, millele vajutades kutsutakse välja kõnetuvastusteenust
pakkuv rakendus, koos oma kasutajaliidesega (nn _RecognizerIntent_).
Kõnele puhul on selleks ülal kirjeldatud
punase nupuga paneel. Teisest rakendusest välja kutsutuna
ei edastata Kõnele tuvastustulemust veebibrauserile, vaid tagastab kutsuvale
rakendusele (nt _Google Keep_), mis siis sellega edasi toimetab.

Kui seadmes on mitu erinevat kõnetuvastusteenust (üheks on tavaliselt _Google'i otsing_),
siis palub Android kõigepealt valida, millist neist kasutada. Valitud teenuse võib
määrata ka vaikimisi valikuks ("alati").

<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-45-10.png">

Juhul kui valikuvõimalust ei tekkinud, st valikudialoogiakent ei kuvatud
ning kohe käivitus nt _Google'i otsing_, siis järelikult oli see
määratud vaikimisi tuvastajaks. Sellise vaikeväärtuse saab eemaldad nõnda:

  - Minge `Seaded -> Rakendused`
  - Valige tab "Kõik"
  - Otsige nimekirjast üles "Google'i otsing"
  - Vajutage nupule "Kustuta vaikeväärtused"

## Kõnele kutsumine teistest rakendustest (taustateenusena)

Androidi rakendused võivad kõnetuvastusteenuse välja kutsuda ka taustateenusena, st ilma kasutajaliideseta.
Nõnda toimivad paljud mikrofoninupuga klaviatuurirakendused ning intelligentsed abilised,
mis hoolitsevad ise kogu kasutajaliidese eest (mikrofoninupp, helisignaalid, VU-meeter, veateadete kuvamine jms).
Selliselt kutsutavad teenused on kirjas menüüs `Seaded -> Keeled ja sisestamine -> Kõne -> Häälsisend`,
kus üks neist on alati seatud vaikimisi teenuseks.

<img src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-21-16-00.png">

Ühe Kõnele teenustest võib seada vaikimisi teenuseks. See ei garanteeri küll kahjuks, et kõik rakendused
hakkavad nüüd kõnetuvastuseks Kõnelet kasutama, sest paljud neist ignoreerivad kasutajamääratud
vaikeväärtust ja kasutavad endiselt nt Google'i teenust.
Selline on olukord paljude klaviatuurirakendustega, milles oleva mikrofoninupu vajutamine
käivitab Google'i kõnetuvastusteenuse, ning seda muuta pole võimalik. Üheks erandiks
on _SlideIT Keyboard_, mida saab konfigureerida Kõnelet kasutama.
Hea ülevaade parimatest klaviatuurirakendustest (eesti keeles kirjutamise
seisukohast) on ajakirjas [[digi] 5/2014](http://www.digi.ee/arhiiv/ajakiri-digi-05-2014).

Huvitav olukord on Google'i tõlkerakendusega (_Google Translate_), mis kasutab kõnetuvastuseks
üldiselt Google'i tuvastajat, kuid keelte jaoks, mida see ei toeta (nt eesti keel)
kasutab vaikimisi määratud kõnetuvastusteenust. Seega saab Kõnele ja Google'i tõlkerakendusega
teha kõnest-kõnesse tõlget eesti keelest paljudesse teistesse keeltesse.

## Grammatika-põhine kõnetuvastus

(Eeldab grammatikatoega teenuse kasutamist)

Kõnele võimaldab igale Androidi rakendustele, mis on Kõnele vähemalt ühe korra välja kutsunud
omistada tõlkegrammatika.  Grammatika omistamisel rakendusele on sisuliselt kaks funktsiooni:

  - deklareerimine, et ainult teatud laused ja sõnavara omab vastava rakenduse kontekstis mõtet, nt mõõtühikute teisendamise rakendus võiks toetada fraase nagu "kaks meetrit jalgades" kuid peaks välistama fraasid nagu "mis ilm on tartus" või "kolm meetrit ruutmeetrites" (viimane kasutab küll õiget sõnavara, kuid teeb seda semantiliselt valel moel). Kui kõnetuvastusserverile sel viisil grammatika esitada, siis on väiksem tõenäosus, et tuvastamisel tehakse viga;
  - tuvastustulemuse "tõlkimine" kujule, mis sobib vastavale rakendusele paremini, nt mõõtühikute teisendamise rakendused eeldavad tüüpiliselt inglise keelest, numbritest ja SI ühikutest/prefiksitest koosnevat sisendit, st "convert 100 m/s to km/h", mitte eestikeelset "kui palju on sada meetrit sekundis kilomeetrites tunnis".

Sellised grammatikad ei kata loomulikku keelt (nt eesti keelt) tervikuna, vaid ainult selle
mingit piiratud alamosa. Nt lauseid mõõtühikute või valuutade teisendamise kohta,
aritmeetika avaldiste keelt, aadressipäringute keelt jne.

Iga grammatika on esitatud HTTP-veebiaadressina, mis tuleb eelnevalt serveris registreerida.
Kõnele seadetes, menüüs "Grammatikad" on loend juba registreeritud grammatikatest.
Grammatika omistamiseks rakendusele tuleb sellele "Rakendused" loendis pikalt vajutada (_long tap_).

Vaatleme näitena grammatika-põhist tuvastust rakenduse _Google Now_ otsingureal.
See rakendus võimaldab loomulikus keeles antud sisendi põhjal teha erinevaid toimingud
(äratuskella helisema panemine, aadressiotsing, mõõtühikute teisendamine, jms), kuid
eesti keelt sisendina ei toeta, st eestikeelse sisendi puhul sooritatakse pelgalt veebiotsing.
Sisendi saab anda otse kõne abil, kuid sel juhul oskab _Google Now_ kasutada ainult
Google'i kõnetuvastajat. Õnneks on sisend võimalik ka klaviatuurilt ning kasutada võib ükskõik
millist klaviatuuri, sh ka Kõnele kõneklaviatuuri.
Järgmised pildid näitavad valuuta konverteerimist, kus sisend
("kaksteist tuhat kolmsada nelikümmend viis norra krooni eurodes") jooksvalt _Google Now_-le
arusaadavale kujule ("convert 12345 NOK to EUR") teisendatakse.

<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-21-58-14.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-21-59-04.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-21-59-19.png">

Selleks, et see nii töötaks peab kõneklaviatuuril olema sisse lülitatud "eesti keel/grammatikatoega".
Samuti, peab _Google Now_ rakendusele ("com.google.android.googlequicksearchbox")
omistama _Action_-grammatika. (Seda omistust kasutab ainult grammatikatoega teenus,
muude teenuste puhul seda ignoreeritakse.)

<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-22-04-05.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-22-04-18.png">
<img src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-21-49-06.png">

Olemasolevate grammatikate kohta on võimalik lugeda aadressil <http://kaljurand.github.io/Grammars/>
ning nende registreerimine ja kasutamine grammatikatoega serveris on kirjeldatud lehel
<http://bark.phon.ioc.ee/speech-api/v1>.
Vt ka rakendust [Arvutaja](http://kaljurand.github.io/Arvutaja/), mis kasutab Kõnelet grammatikatoega kõnetuvastajana.

## Veaolukorrad

Kõnele kasutamine ebaõnnestub järgmistes olukordades:

- võrguühendus serverini puudub või on liiga aeglane;
- server ei tööta või on ülekoormatud;
- rakendusel pole ligipääsu mikrofonile, sest mõni teine rakendus parasjagu lindistab.

Nendes olukordades väljastab Kõnele vastava veateate.
Muud sorti vead palun raporteerida aadressil <http://github.com/Kaljurand/K6nele/issues>
või kirjutades <mailto:kaljurand+k6nele@gmail.com>.

Tagasiside sellele juhendile palun jätta allolevasse kasti.
