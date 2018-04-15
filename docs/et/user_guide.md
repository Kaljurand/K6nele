---
layout: page
comments: true
title: Kasutusjuhend
---

- TOC
{:toc}

## Sissejuhatus

__Kõnele__ on kõnetuvastusteenus Androidi rakendustele, mis võimaldab kõne abil
sooritada infootsingut,
kirjutada e-kirju ja märkmeid,
anda käske jne.

- Paljudes Androidi rakendustes on tekstikastid, millele vajutades avaneb klaviatuurirakendus.
Kõnele sisaldab endas klaviatuurirakendust, mis võimaldab kõnesisendit.

- Paljudes Androidi rakendustes on väike mikrofoninupp, millele vajutades eeldab
rakendus kasutajalt mõnesekundilist kõnejuppi, mis automaatselt tekstiks teisendatakse.
See mikrofoninupp on tihti seotud Androidi avatud kõnetuvastusliidesega,
läbi mille on võimalik kasutada ka Kõnele poolt pakutud tuvastust.

- Kui käivitada Kõnele otse, st mitte läbi teise rakenduse,
siis suunatakse tuvastatud tekst (vaikimisi) edasi veebiotsingumootorile, või soovi korral (ümberkirjutusreeglite abil) nt kodurobotile.

Kõnele kasutab kõne tekstiks teisendamiseks ehk transkribeerimiseks [TTÜ Küberneetika Instituudi
foneetika ja kõnetehnoloogia laboris](http://phon.ioc.ee) välja töötatud
serveripõhist kõnetuvastustarkvara, mis on maailmas hetkel ainus, mis
sisendina eesti keelt toetab.
Samuti toetab see grammatikapõhist kõnetuvastust,
lubades kasutajal täpselt defineerida,
milliseid sõnu ja lauseid ta erinevates rakendustes kasutab.

Kuna Kõnele kasutab tööks veebiserverit, peab olema nutiseadmes internetiühendus sisse lülitatud.
Sõltuvalt mobiilioperaatori teenusepaketist võib interneti kasutamise hind
sõltuda andmemahtudest. Seega tasub teada, et pooleminutise kõne
transkribeerimiseks laaditakse serverisse umbes 1MB jagu andmeid. Wifivõrkudes
on Kõnele kasutuskiirus tüüpiliselt oluliselt parem kui 3G jms võrkudes.

Järgnev juhend kirjeldab Kõnele seadistamist ja kasutamist eestikeelse kasutajaliidesega
Android v5 (Lollipop) ja uuemates seadmetes. Androidi kasutajaliidese tõlge ja struktuur on seadmeti ja versiooniti mõnevõrra erinev, kuid mitte oluliselt.

## Demovideo

Järgnev video näitab
(1) kõnepõhist veebiotsingut;
(2) kõneklaviatuuri sisse lülitamist ja sellega kirja kirjutamist;
(3) aadressiotsingut kaardirakenduses;
(4) Kõnele konfigureerimist Androidi vaikimisi kõnetuvastajaks, ja selle kasutamist _Google Translate_ rakenduses;
(5) tõlkegrammatika omistamist _Wolfram Alpha_ rakendusele, ja selles mõõtühikuteisendamist;
(6) _Arvutaja_ rakenduse kasutamist (vahelduseks inglise keeles) äratuskella seadmiseks.
Video on tehtud Kõnele versiooniga 0.8.56, uuemates versioonides on kasutajaliides natuke
muutunud.

<iframe id="ytplayer" type="text/html" width="480" height="360"
src="http://www.youtube.com/embed/gKFIWSA2GWc?origin=http://kaljurand.github.io"
frameborder="0"> </iframe>

Allpool tuleb kõikidest nendest kasutusolukordadest lähemalt juttu.

## Kõnele kui iseseisev rakendus

Vajutades Kõnele käivitusikoonile (_launcher icon_) avaneb mikrofoninupuga paneel.
Nupule vajutades teisendab Kõnele sisendkõne tekstiks ning edastab selle
edasi seadme veebibrauserile.

<img title="Ekraanipilt: käivitusikoon" alt="Ekraanipilt: käivitusikoon." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-23-09-01-50.png">
<img title="Ekraanipilt: mikrofoninupuga paneel" alt="Ekraanipilt: mikrofoninupuga paneel." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-23-09-01-59.png">
<img title="Ekraanipilt: lindistamine" alt="Ekraanipilt: lindistamine ja transkribeerimine." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-23-09-02-10.png">
<img title="Ekraanipilt: transkribeerimine" alt="Ekraanipilt: transkribeerimine peale lindistamise lõppu." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-23-09-02-27.png">
<img title="Ekraanipilt: valik tuvastustulemusi" alt="Ekraanipilt: valik tuvastustulemusi." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-23-09-02-35.png">
<img title="Ekraanipilt: tuvastustulemus veebiotsinguna" alt="Ekraanipilt: tuvastustulemus veebiotsinguna." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-23-09-02-48.png">

Ümberkirjutusreeglid võimaldavad tuvastustulemust muuta ning avada see muus rakenduses kui veebibrauser. Näiteks lisab järgmine reegel transkriptsioonile sõne `, Estonia` ning avab tulemuse [kaardirakenduses](https://developer.android.com/guide/components/intents-common.html#Maps).

{% highlight sh %}
    Utterance<HT>Command<HT>Arg1<NL>
    (.*)<HT>activity<HT>{"action":"android.intent.action.VIEW", "data":"geo:0,0?q=$1, Estonia"}
{% endhighlight %}

Antud juhul on reeglil kolm komponenti: regulaaravaldis, mis vastab lausungile (nt ``(.*)`` vastab suvalisele lausungile); käsk, mis käivitab rakenduse (``activity``) ning käsu argument (JSON struktuuriga kirjeldatud Androidi Intent, mis viitab ``$1`` abil kasutaja sisendile). Komponente eraldab tabulaator (ülal tähistatud kui ``<HT>``). Reeglifaili read on eraldatud reavahetusega (ülal tähistatud kui ``<NL>``) ning esimene rida on päis. Ümberkirjutusreegleid käsitleb pikemalt eraldi peatükk.


## Kõnele seadistamine

<img style="float: right" title="Ekraanipilt: seadistamine" alt="Ekraanipilt: seadistamine." src="{{ site.baseurl }}/images/et/Screenshot_2017-09-13-23-06-39.jpg">

Sama mikrofoninupuga paneeli ülemises paremas nurgas on nupp, mis viib Kõnele seadetesse.
Need võimaldavad Kõnele töökäiku erinevatel viisidel suunata, määrates nt

- milliseid tuvastusteenusi ja -keeli kasutatakse;
- kui palju tuvastushüpoteese serverilt paluda;
- kas lindistamine algab automaatselt või peale nupule vajutamist;
- kas lindistamine lõpeb kui sisendkõnesse tekib paus, või alles siis, kui nupule on vajutatud;
- kas lindistamise algusest ja lõpust teavitatakse lühikese helisignaaliga;
- kas/kuidas rakendada tuvastustulemusele tõlkegrammatikaid ja ümberkirjutusreegleid (vt allpool).

Mõned nendest seadetest puudutavad ainult Kõnele enda kasutajaliidest (st klaviatuuri ja otsingupaneeli) ning
seega ei rakendu juhul kui Kõnelet kasutatakse läbi teise rakenduse.

Kõnele toetab kahte erinevat kõnetuvastusteenust:

  - "grammatikatoega" teenus (kasutab serverit tarkvaraga <http://github.com/alumae/ruby-pocketsphinx-server>)
    lubab sisendkõnele omistada tõlkegrammatikaid, kuid on aeglasem ja sisendkõne pikkus ei tohi ületada
    30 sekundit;
  - "kiire tuvastusega" teenus (kasutab serverit tarkvaraga <http://github.com/alumae/kaldi-gstreamer-server>)
    tagastab tuvastustulemuse juba rääkimise ajal, ega sea sisendkõne pikkusele mingit piirangut.

Mõlema teenuse tarkvara on vaba lähtekoodiga ja teenuse veebiaadressid on Kõneles konfigureeritavad. Seega võib teenuse installeerida suurema kiiruse ja privaatsuse huvides kohtvõrku.

Kõnele kasutajaliidesed kasutavad vaikimisi "kiire tuvastusega" kõnetuvastusteenust, kuid
lisada saab ka teisi seadmesse installeeritud teenuseid ja nende poolt toetatud
keeli (nt Kõnele "grammatikatoega" teenus ja Google'i teenus),
muutes vastavat seadet.
Kui valitud on mitu keelt/teenust, siis on Kõnele mikrofoninupu juures lisaks keele/teenuse vahetamise nupp:

- lühike vajus lülitab järgmisele keelele/teenusele,
- pikk vajutus kuvab kõik võimalikud keeled/teenused, võimaldades valikut muuta.

(Vt näidet peatükis "Grammatikapõhine kõnetuvastus".)

Otsingupaneeli jaoks välja valitud keeled/teenused on saadaval ka otselinkidena (_app shortcuts_), alates Android v7.1. Otselingid avanevad kui näppu pikemalt Kõnele käivitusikoonil hoida, samuti võib otselingi teha ikooniks. Otselingil klikkides alustab Kõnele koheselt tuvastamist väljavalitud keeles/teenuses.

<img title="Ekraanipilt: otselingid" alt="Ekraanipilt: otselingid." src="{{ site.baseurl }}/images/et/Screenshot_20161227-115800.png">

Otselink on lihtne näide, kuidas käivitada Kõnele otsingupaneel sisendparameetritega, mille väärtused erinevad nendest, mis seadetes kirjas. Kõnele toetab palju erinevaid sisendparameeterid (nn EXTRA), mis võimaldavad teistel rakendustel (nt [Tasker](https://tasker.dinglisch.net/)) Kõnelega otse suhelda. Vt täpsemalt [Developer's Guide]({{ site.baseurl }}/docs/en/developer_guide.html).

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
tähtede ja numbritega klahvistikust on Kõnele klaviatuuril ainult paar klahvi,
ning kogu tekstisisestus toimub kõne abil.

<img title="Ekraanipilt: klaviatuur ja otsingurida" alt="Ekraanipilt: klaviatuur ja otsingurida." src="{{ site.baseurl }}/images/et/Screenshot_2015-06-14-00-15-41.png">
<img title="Ekraanipilt: klaviatuur ja märkmerakendus" alt="Ekraanipilt: klaviatuur ja märkmerakendus." src="{{ site.baseurl }}/images/et/Screenshot_2015-06-14-00-23-30.png">

### Seadistamine

Kõnele klaviatuuri kasutamiseks tuleb see ennem sisse lülitada Androidi süsteemses
virtuaalklaviatuurimenüüs.
Kui Kõnele klaviatuur pole sisse lülitatud, siis on Kõnele seadetes, esimesel kohal, ka
otselink sellesse menüüsse.

<img title="Ekraanipilt: seadistamine" alt="Ekraanipilt: seadistamine." src="{{ site.baseurl }}/images/et/IMG_20170916_113335.png">

Androidi seadete hierarhias asub vastav menüü üsna sügaval, ning lisaks erineb selle asukoht
Androidi versiooniti:

- Android v5: `Seaded -> Keeled ja sisestamine -> Klaviatuur ja sisestusmeetodid`
- uuemad Androidi versioonid: `Seaded -> Täpsemad seaded -> Keel ja klahvistik -> Virtuaalne klaviatuur`

Järgnevad ekraanipildid näitavad klaviatuuri seadistamist Androidis v5.

<img title="Ekraanipilt: Androidi sisestusmeetodite seadistamine" alt="Ekraanipilt: Androidi sisestusmeetodite seadistamine." src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-32-57.png">
<img title="Ekraanipilt: Androidi vaikeklaviatuuri muutmine" alt="Ekraanipilt: Androidi vaikeklaviatuuri muutmine." src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-33-10.png">
<img title="Ekraanipilt: nimekiri sisselülitatud klaviatuuridest" alt="Ekraanipilt: nimekiri sisselülitatud klaviatuuridest." src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-33-51.png">
<img title="Ekraanipilt: klaviatuuri sisselülitamine" alt="Ekraanipilt: klaviatuuri sisselülitamine." src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-34-00.png">
<img title="Ekraanipilt: Kõnele on määratud vaikeklaviatuuriks" alt="Ekraanipilt: Kõnele on määratud vaikeklaviatuuriks." src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-19-34-24.png">

### Kasutamine

Kõiki sisselülitatud klaviatuure on võimalik paralleelselt kasutada --- tekstikastile klikkides
kuvatakse vaikeklaviatuur ("praegune klaviatuur"), kuid ekraani alumises nurgas asuva klaviatuurinupu (põhinuppude _Back_, _Home_, _Recent apps_ kõrval) läbi saab seda jooksvalt muuta.
Kõnele klaviatuuri ongi mõistlik kasutada paralleelselt mõne "tavaklaviatuuriga"
(nt _Gboard_, _Swype_, _SwiftKey_, _SlideIT_).
Kõnetuvastuse abil tekstide dikteerimine sobib peamiselt olukordadesse,
kus keskkond on vaikne ja privaatne, ja tekst ei pea olema keeleliselt täiesti perfektne.
Sellistes olukordades on kõnetuvastuse kasutamine reeglina kiirem ja loomulikum
ning lisaks võtab kõneklaviatuur ekraanil vähem ruumi.
Muudes olukordades võib ümber lülitada teisele klaviatuurile.
Mõned klaviatuurid (nt _Gboard_ ja _Kõnele_ ise)
võimaldavad klaviatuurivahetust ainult ühe nupuvajutusega. Nt, vajutades maakera-ikooni
_Gboard_ klaviatuuril vahetub klaviatuur _Kõnele_ vastu; vajutades klaviatuuri-ikooni
_Kõnele_ klaviatuuril, vahetub klaviatuur tagasi _Gboard_ klaviatuurile.
Selles rotatsioonis võib osaleda ka rohkem klaviatuure,
kui nad samamoodi vastavat Androidi klaviatuurivahetusliidest toetavad.

<img title="Ekraanipilt: traditsiooniline klaviatuur katab pool märkmerakendusest" alt="Ekraanipilt: traditsiooniline klaviatuur katab pool märkmerakendusest." src="{{ site.baseurl }}/images/et/Screenshot_2015-05-11-21-44-34.png">&harr;<img title="Ekraanipilt: Kõnele katab ainult veerandi märkmerakendusest" src="{{ site.baseurl }}/images/et/Screenshot_2015-06-14-01-46-37.png">

### Omadused

Lisaks nupule, mis käivitab/lõpetab/katkestab kõnetuvastuse, toetab Kõnele
klaviatuur järgmisi operatsioone:

- vajutus klaviatuuriikoonile vahetab eelmisele klaviatuurile;
- pikk vajutus klaviatuuriikoonile vahetab järgmisele klaviatuurile;
- vajutus otsinguikoonile käivitab otsingu (ainult otsingureal),
  (alates v1.6.78 on otsinguikooni asemel tekstivälja tüübist sõltuva funktsiooni ja ikooniga nupp, mis
  sooritab otsingu, lisab reavahetuse, või liigutab kursori järgmisele väljale);
- topeltvajutus lisab tühiku;
- variant 1 (vaikimisi):

  - svaip vasakule kustutab kursorist vasakul asuva sõna,
  - svaip paremale lisab reavahetuse,
  - pikk vajutus valib kogu teksti;

- variant 2 (valikuline alates v1.6.78):

  - kustutamise ikoon kustutab kursorist vasakul oleva sümboli või praeguse valiku,
  - svaip vasakule liigutab kursori vasakule (svaip vasakule üles teeb sama kiiremini),
  - svaip paremale liigutab kursori paremale (svaip paremale alla teeb sama kiiremini),
  - pikk vajutus valib kursori all/kõrval oleva sõna ning siseneb valiku-režiimi, kus svaibid muudavad valiku ulatust,
  - pidev vajutus klaviatuuri vasakule äärele liigutab kursorit vasakule,
  - pidev vajutus klaviatuuri paremale äärele liigutab kursorit paremale.

Lisaks on võimalik teksti sisestada ja muuta [ümberkirjutusreeglite](#ümberkirjutusreeglid) abil (vt allpool).

## Kõnele kutsumine teistest rakendustest

### Koos kasutajaliidesega

Mõnes rakenduses (nt _Google Keep_) on mikrofoninupp, millele vajutades kutsutakse välja kõnetuvastusteenust
pakkuv rakendus, koos oma kasutajaliidesega (nn _RecognizerIntent_).
Kõnele puhul on selleks ülal kirjeldatud
mikrofoninupuga paneel. Teisest rakendusest välja kutsutuna
ei edastata Kõnele tuvastustulemust veebibrauserile, vaid tagastab kutsuvale
rakendusele (nt _Google Keep_), mis siis sellega edasi toimetab.

Kui seadmes on mitu erinevat kõnetuvastusteenust (üheks on tavaliselt _Google'i rakendus_),
siis palub Android kõigepealt valida, millist neist kasutada. Valitud teenuse võib
määrata ka vaikimisi valikuks (`ALATI`).

<img title="Ekraanipilt: mikrofoninupuga märkmerakendus" alt="Ekraanipilt: mikrofoninupuga märkmerakendus." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-27-14-39-17.png">
<img title="Ekraanipilt: tuvastusteenust pakkuvate rakenduste valik" alt="Ekraanipilt: tuvastusteenust pakkuvate rakenduste valik." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-27-14-39-32.png">
<img title="Ekraanipilt: Kõnele otsingupaneel märkmerakendusele tuvastusteenuse pakkujana" alt="Ekraanipilt: Kõnele otsingupaneel märkmerakendusele tuvastusteenuse pakkujana." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-27-14-39-45.png">

Juhul kui valikuvõimalust ei tekkinud, st valikudialoogiakent ei kuvatud
ning kohe käivitus nt _Google'i rakendus_, siis järelikult oli see
määratud vaikimisi tuvastajaks. Sellise vaikeväärtuse saab eemaldad nõnda:

  - Minge `Seaded -> Rakendused`
  - Valige tab `Kõik`
  - Otsige nimekirjast üles "Google'i rakendus"
  - Vajutage nupule `Kustuta vaikeväärtused`

### Taustateenusena

Androidi rakendused võivad kõnetuvastusteenuse välja kutsuda ka taustateenusena, st ilma kasutajaliideseta.
Nõnda toimivad paljud mikrofoninupuga klaviatuurirakendused ning intelligentsed abilised,
mis hoolitsevad ise kogu kasutajaliidese eest (mikrofoninupp, helisignaalid, VU-meeter, veateadete kuvamine jms).
Selliselt kutsutavad teenused on kirjas süsteemses `Häälsisend`-menüüs,
kus üks neist on alati seatud vaikimisi teenuseks.
(Android v4-5: `Seaded -> Keeled ja sisestamine -> Kõne -> Häälsisend`;
Android v6-7: `Seaded -> Rakendused -> Rakenduste seadistamine (hammasrattaikoon) -> Vaikerakendused -> Abirakendus ja häälsisend -> Häälsisend`; Kõnele seadetes on ka otsetee `Kõik kõnetuvastusteenused`.)


<img title="Ekraanipilt: nimekiri paigaldatud tuvastusteenustest" alt="Ekraanipilt: nimekiri paigaldatud tuvastusteenustest." src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-21-16-00.png">

Ühe Kõnele teenustest võib seada vaikimisi teenuseks. See ei garanteeri küll kahjuks, et kõik rakendused
hakkavad nüüd kõnetuvastuseks Kõnelet kasutama, sest paljud neist ignoreerivad kasutajamääratud
vaikeväärtust ja kasutavad endiselt nt Google'i teenust.
Selline on olukord paljude klaviatuurirakendustega, milles oleva mikrofoninupu vajutamine
käivitab Google'i kõnetuvastusteenuse, ning seda muuta pole võimalik. Üheks erandiks
on _SlideIT Keyboard_, mida saab konfigureerida Kõnelet kasutama.
Hea ülevaade parimatest klaviatuurirakendustest (eesti keeles kirjutamise
seisukohast) on ajakirjas [[digi] 5/2014](http://www.digi.ee/2014/05/arhiiv/).

Huvitav olukord on Google'i tõlkerakendusega (_Google Translate_), mis kasutab kõnetuvastuseks
üldiselt Google'i tuvastajat, kuid keelte jaoks, mida see ei toeta (nt eesti keel)
kasutab vaikimisi määratud kõnetuvastusteenust. Seega saab Kõnele ja Google'i tõlkerakendusega
teha kõnest-kõnesse tõlget eesti keelest paljudesse teistesse keeltesse.

Android v6 on lisanud nn "Abirakenduse" mõiste, kuid kui abirakenduseks on valitud _Google'i rakendus_, siis valikut `Häälsisend` ei kuvata, ning Kõnele rakendust vaikimisi teenuseks seada ei saa. Üheks kahetsusväärseks tagajärjeks on see, et nüüd puudub võimalus korraga kasutada funktsionaalsust "Google Assistant" ja eestikeelset kõnesisendit rakenduses _Google Translate_.
(Vt ka vearaporteid [200494](https://code.google.com/p/android/issues/detail?id=200494)
ja [200496](https://code.google.com/p/android/issues/detail?id=200496).)

## Ümberkirjutusreeglid

Ümberkirjutusreeglid on Kõnele kasutaja poolt loodavad reeglid tuvastusteenuse poolt tagastatud transkriptsiooni jooksvaks muutmiseks, ja sellele käskude rakendamiseks.
(Soovi korral vaata kohe [näiteid reeglitest](#näited).)

Ümberkirjutusreeglid võimaldavad

- sisestada halvasti tuvastatavaid sõnu (nt pärisnimesid), ja parandada muid tuvastaja võimalikke puudujääke (autopunktsiooni puudumine, emotikonide toe puudumine, jms);
- sisestada tekste, mis ei kipu meelde jääma, või mida ei taha tuvastajale avaldada (nt telefoninumbrid, aadressid, kontonumbrid);
- sisestada korduma kippuvaid pikemaid tekste;
- käivitada teisi Androidi rakendusi;
- rakendada tekstitoimetuskäske juba sisestatud teksti muutmiseks.

Kõnele laeb ümberkirjutusreeglid lihtsast tabelikujulisest tekstifailist, nn TSV-failist, kus veerueraldajaks on tabulaator ja reaeraldajaks reavahetussümbol. Kõnele toetab järgmisi veerge (muid ignoreerib):

- __Utterance__ Regulaaravaldis kõnesisendi tuvastamiseks, st lausungimuster, millele vastab üks või rohkem võimalikku kõnesisendit. Võib sisaldada alamgruppe (nn _capturing group_), mis on tähistatud sulgudega `()` ja viiteid nendele (tähistatud `\1`, `\2`, ...).
- __Replacement__ Asendustekst. Võib sisaldada viiteid __Utterance__ gruppidele (tähistatud `$1`, `$2`, ...).
- __Locale__ Regulaaravaldis keele/riigi (nn _locale_) kirjeldusega (nt `et`, `en-US`).
- __Service__ Regulaaravaldis tuvastusteenuse Java klassi nime kirjeldusega.
- __App__ Regulaaravaldis rakenduse paki nime kirjeldusega, milles Kõnelet kasutatakse.
- __Comment__ Rida kirjeldav kommentaar.

Igale reale vastab üks reegel, ning ridade järjekord määrab reeglite rakendamise järjekorra. Nõnda saavad allpool olevad reeglid ära kasutada eelnevate reeglite ümberkirjutusi.
Veergude järjekord pole oluline. Kohustuslik veerg on ainult __Utterance__. Kui __Replacement__ puudub, siis on asendustekst alati tühisõne. Veerud __Locale__, __Service__ ja __App__ määravad, millise keele, rakenduse, ja tuvastusteenuse puhul on reegel aktiivne. Kõik regulaaravaldised on [Java regulaaravaldised](https://docs.oracle.com/javase/tutorial/essential/regex/). Põhjalik regulaaravaldiste õpetus on nt <http://www.regular-expressions.info>.

Veergude tüübid on määratud esimesse ritta (nn päisesse) kirjutatud ingliskeelse märksõnaga ("Utterance", "Replacement", jne). Kui päis puudub (st esimene rida ei sisalda kohustusliku veeru nime "Utterance"), siis arvestatakse ainult tabeli esimest kahte veergu ning interpreteeritakse neid kui __Utterance__ ja __Replacement__ veerge. Kui tabelil on ainult üks veerg, siis on __Replacement__ alati tühisõne. Seega, kõige lihtsam tabel koosneb ainult ühest sümbolist, nt ``a`` (kustuta kõik "a" tähed).

Näide. Lihtne (eestikeelne) ümberkirjutusreegel. Küsimärk lausungimustris määrab igaks juhuks, et tühik on lausungis valikuline. Nõnda ei sõltu reegli rakendmine sellest, kuidas tuvastaja sõnu kokku/lahku kirjutab.

- __Locale__ = `et`
- __Utterance__ = `minu lemmik ?matemaatiku ?nimi`
- __Replacement__ = `Srinivasa Ramanujan`

Näide. Pikema teksti sisestamine. Märgid `^` ja `$` nõuavad, et lausung vastaks mustrile algusest lõpuni. Asendustekstis olevad `\n` märgid tähistavad reavahetust, ning nurksulud on lisatud selleks, et hiljem oleks lihtsam tekstis veel täitmist vajavate osade juurde navigeerida (nt käsuga "vali klambrid").

- __Locale__ = `et`
- __Utterance__ = `^vastuse vorm müügipakkumisele$`
- __Replacement__ = `Lugupeetud []\n\nTäname Teid, et vastasite meie pakkumisele.\n\nLugupidamisega\nHeikki Papper\nmüügijuht`

Näide. Keelest sõltumatu reegel, mis tuvastab lausungis kahe järjestikuse sõna kordumise (nt "eks ole eks ole"), ja eemaldab korduse. Muster `[^ ]+ [^ ]+` kirjeldab ühte tühikut sisaldavat sõne (st kahte sõna) ja sulud selle ümber teevad ta viidatavaks `\1` ja `$1` abil.

- __Utterance__ = `([^ ]+ [^ ]+) \1`
- __Replacement__ = `$1`

### Käsud

(_Eksperimentaalne_)

Käskude sidumiseks lausungiga tuleb kasutada kuni kolme lisaveergu:

- __Command__ Käsu nimi (ingliskeelne märksõna).
- __Arg1__ Käsu esimene argument (valikuline).
- __Arg2__ Käsu teine argument (valikuline).

Argumendid võivad sisaldada viiteid __Utterance__ gruppidele (`$1`, `$2`, ...).

Näide. Eestikeelne kõnekäsk (nt `ärata mind kell 8 0 5 mine tööle`) äratuskella helisema panemiseks.
Reegel eraldab lausungist vajalikud argumendid (tundide arv `8`, minutite arv `5`, täpsustav kommentaar `mine tööle`) ning
loob nende põhjal [JSON](http://www.json.org/)-struktuuri. Käsk `activity` püüab interpreteerida seda struktuuri kui Androidi
[Intent](https://developer.android.com/reference/android/content/Intent.html) kirjeldust. Kui see õnnestub,
siis püüab leida _Intent_'ile vastava rakenduse ning selle käivitada.

- __Locale__ = `et`
- __Utterance__ = `^ärata mind(?: palun)? kell (\d+) (?:0 )?(\d+)\s*(.*)$`
- __Command__ = `activity`
- __Arg1__ =

{% highlight json %}
         {
             "action": "android.intent.action.SET_ALARM",
             "extras": {
                 "android.intent.extra.alarm.HOUR": $1,
                 "android.intent.extra.alarm.MINUTES": $2,
                 "android.intent.extra.alarm.MESSAGE": "$3",
                 "android.intent.extra.alarm.SKIP_UI": true
             }
         }
{% endhighlight %}

(Loetavuse huvides on __Arg1__ näites kasutatud reavahetusi. Reeglitabelis eraldab reavahetus reegleid, seega ei tohi ükski tabelilahter reavahetussümbolit sisaldada.)

#### Tekstitoimetuskäsud

Tekstitoimetuskäsud on käsud, mida saab kasutada ainult koos Kõnele klaviatuuriga.
Need võimaldavad toimetada juba olemasolevat teksti käed vabalt (st ainult kõne abil), nt kursori liigutamist teksti sees ja väljade vahel (nt `selectReBefore`, `keyUp`, `imeActionNext`), sõnade/lausete valimist ja asendamist (nt `select`, `selectReAfter`, `replace`), operatsioone valikuga (nt `replaceSel`, `saveClip`), lõika/kleebi/kopeeri operatsioone, [Androidi IME käske](https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html) (nt `imeActionSend`). Enamikku käskudest on võimalik tagasi võtta (`undo`), mitu korda rakendada (`apply`), ja isegi kombineerida (`combine`). Argumendid võivad viidata parasjagu aktiivse valiku sisule funktsiooniga `@sel()`. Kursoriliigutamiskäskude puhul, mille argumendiks on regulaaravaldis (`..Re..`), määrab selle esimene alamgrupp kursori uue asukoha.
Vt ka [kõikide tekstitoimetuskäskude nimekiri](https://github.com/Kaljurand/speechutils/blob/master/app/src/main/java/ee/ioc/phon/android/speechutils/editor/CommandEditor.java).

Näide. (Eestikeelne) kõnekäsk lisamaks valitud tekstilõigu ümber nurksulud. Muid sõnu väljundisse ei lisata, kuna __Replacement__ on tühisõne.

- __Locale__ = `et`
- __Utterance__ = `lisa ?klambrid`
- __Replacement__ =
- __Command__ = `replaceSel`
- __Arg1__ = `[@sel()]`

Näide. (Eestikeelne) kõnekäsk "saatmisnupu" vajutamiseks Google'i rakendustes Hangouts (mille pakinimi sisaldab sõne "talk") ja Allo ("fireball"). Lausungimuster sisaldab suvalist teksti `.*`, mida eraldab käsust `saada ära` sõnapiir `\b`. Ennem käsu (`imeActionSend`) täitmist lisatakse tekst väljundisse.

- __Locale__ = `et`
- __App__ = `google.*(talk|fireball)`
- __Utterance__ = `(.*)\bsaada ära`
- __Replacement__ = `$1`
- __Command__ = `imeActionSend`

Näide. (Eestikeelne) kõnekäsk, mis rakendab lausele vastavat mustrit (st sõne, mis algab ja lõpeb lauselõpumärgiga) kursorile järgnevale tekstile, ning liigutab kursori mustri teise esinemise keskele (pärast lauselõpu märki ja valikulist tühikut).

- __Locale__ = `et`
- __Utterance__ = `mine ülejärgmise lause algusesse`
- __Replacement__ =
- __Command__ = `selectReAfter`
- __Arg1__ = `[.?!]\\s*()[^.?!]+[.?!]`
- __Arg2__ = `2`

### Reeglite tegemine

Reeglifaili loomiseks ja salvestamiseks sobib iga tabelarvutusprogramm. Nt [Google'i Arvutustabelid](https://www.google.com/intl/et/sheets/about/) (_Google Sheets_) võimaldab selliseid tabeleid luua nii lauaarvutis kui ka mobiiliseadmes, ning siis erinevate seadmete ja kasutajate vahel TSV-kujul jagada. Faili laadimiseks Kõnele rakendusse on kaks võimalust:

- Kõnele menüüvalik "Ümberkirjutusreeglid" avab nimekirja olemasolevatest reeglistikest. Seal on Lisa-nupp (plussmärk ringi sees), mis avab failibrauseri, mille abil tuleb soovitava faili juurde navigeerida ning sellele klikkida.
- Tabelarvutusrakenduses on failijagamislink, millele klikkides avaneb võimalus faili TSV-kujule teisendamiseks ning tulemuse jagamiseks Kõnelega. Järgnevad ekraanipildid näitavad faili jagamist rakenduses Google'i Arvutustabelid, menüüde "Jagamine ja eksportimine" ja "Saada koopia" abil.

<img title="Ekraanipilt: ümberkirjutusreeglid tabelarvutusrakenduses" alt="Ekraanipilt: ümberkirjutusreeglid tabelarvutusrakenduses." src="{{ site.baseurl }}/images/et/Screenshot_20160925-202955.png">
<img title="Ekraanipilt: ümberkirjutusreeglite jagamine menüüvalikuga 'Jagamine ja eksportimine'" alt="Ekraanipilt: ümberkirjutusreeglite jagamine menüüvalikuga 'Jagamine ja eksportimine'." src="{{ site.baseurl }}/images/et/Screenshot_20160925-203014.png">
<img title="Ekraanipilt: ümberkirjutusreeglite jagamine menüüvalikuga 'Saada koopia'" alt="Ekraanipilt: ümberkirjutusreeglite jagamine menüüvalikuga 'Saada koopia'." src="{{ site.baseurl }}/images/et/Screenshot_20160925-203027.png">
<img title="Ekraanipilt: ümberkirjutusreeglite teisendamine TSV-formaati" alt="Ekraanipilt: ümberkirjutusreeglite teisendamine TSV-formaati." src="{{ site.baseurl }}/images/et/Screenshot_20160925-203041.png">
<img title="Ekraanipilt: ümberkirjutusreeglite importimine Kõnele rakendusse" alt="Ekraanipilt: ümberkirjutusreeglite importimine Kõnele rakendusse." src="{{ site.baseurl }}/images/et/Screenshot_20170115-154706.png">
<img title="Ekraanipilt: imporditud ümberkirjutusreeglite nimekiri" alt="Ekraanipilt: imporditud ümberkirjutusreeglite nimekiri." src="{{ site.baseurl }}/images/et/Screenshot_20170115-160910.png">

Reeglifaili kasutamiseks tuleb see eelnevalt aktiveerida. Korraga saab aktiivne olla ainult üks reeglifail.

### Näited

- [[TSV]({{ site.baseurl }}/rewrites/tsv/k6_skill_map.tsv), [Sheets](https://docs.google.com/spreadsheets/d/1liMiWDiU4iN1faAENtAIbFenbtpjKocJvNxjyuW9hqU/edit?usp=sharing)] Ühest reeglist koosnev reeglistik, mis näitab, kuidas avada veebibrauseri asemel kaardirakendus.
- [[TSV](https://docs.google.com/spreadsheets/d/1TC7hGq9SDrpiDmRjCxvFzfi6GJwOgKpQDQqTm086Xuk/export?format=tsv), [Sheets](https://docs.google.com/spreadsheets/d/1TC7hGq9SDrpiDmRjCxvFzfi6GJwOgKpQDQqTm086Xuk/edit?usp=sharing)] Tekstiasendusreeglid kirjavahemärkide lisamiseks (kommentaaridega)
- [[TSV]({{ site.baseurl }}/rewrites/tsv/k6_various.tsv), [Sheets](https://docs.google.com/spreadsheets/d/1SXxXcJf6YQv7ALb_2QJWPs9tVsk4SGc-vxSy6n6l1S0/edit?usp=sharing)] Suur hulk lihtsamaid tekstiasendusreegleid, keerulisemaid tekstitoimetusreegleid, ja muid näiteid.
- [[TSV]({{ site.baseurl }}/rewrites/tsv/k6_skill_translate.tsv), [Sheets](https://docs.google.com/spreadsheets/d/1ndVmgLCG1wZ0cedfaAhL_kzw9aoqyP5jnsp1I-qFHwQ/edit?usp=sharing)] Mitut sisendkeelt toetav reeglistik tõlkerakenduse avamiseks koos etteantud keelepaari ja tõlgitava fraasiga. Sisendkeele määrab __Locale__-veerg, väljundkeele ning tõlgitava fraasi määrab lausung.
- [[TSV]({{ site.baseurl }}/rewrites/tsv/k6_skill_send.tsv), [Sheets](https://docs.google.com/spreadsheets/d/1a_waZskhCxM0NGy6T0_cIAzWd7rHocg0kBvFAIJ6M2s/edit?usp=sharing)] Dialoogisüsteem e-kirja saatmiseks, mis näitab, kuidas "programmeerida" ümberkirjutusreeglite abil lihtne dialoogisüsteem.
- [[TSV](https://docs.google.com/spreadsheets/d/1ZAlBIZniTNorGn8U_WwOxNURT9NlyiGfzjGslIbNx2k/export?format=tsv), [Sheets](https://docs.google.com/spreadsheets/d/1ZAlBIZniTNorGn8U_WwOxNURT9NlyiGfzjGslIbNx2k/edit?usp=sharing)] Kõnekäsud valgustite juhtimiseks näitab, kuidas loomulikus keeles sisend viia lihtsamale ja keelest sõltumatule kujule (nt "pane elutoa lamp põlema heledusega kaks sada" -> ``<lights><1><on><200>``), mida alljärgnevad reeglistikud edasi teistendavad ning lõpuks käivitavad.
  - [[TSV](https://docs.google.com/spreadsheets/d/1owXRMDRIGvi4Ya0lP6_LXsbZXs-sslwhzEye5pGAXbo/export?format=tsv), [Sheets](https://docs.google.com/spreadsheets/d/1owXRMDRIGvi4Ya0lP6_LXsbZXs-sslwhzEye5pGAXbo/edit?usp=sharing)] Käivitab formaalsel kujul käske otse Philips Hue silla kaudu, näidates, kuidas Kõnele abil HTTP-päringuid teha.
  - [[TSV](https://docs.google.com/spreadsheets/d/1lxvkGerd_WMljca0dsgxViw_5cnOEgDzneBL-uXI-xI/export?format=tsv), [Sheets](https://docs.google.com/spreadsheets/d/1lxvkGerd_WMljca0dsgxViw_5cnOEgDzneBL-uXI-xI/edit?usp=sharing)] Käivitab formaalsel kujul käske [Home Assistant](https://home-assistant.io/) abil. Home Assistant toetab nii Philips Hue, kui ka teiste tootjate valgusteid, samuti palju muid targa kodu seadmeid, tehes need kättesaadavaks lihtsa HTTP-liidese kaudu.
- [[TSV](https://docs.google.com/spreadsheets/d/1ViO4swIovvuRJC-kiPaQOIdAkuwHCbQvTQlNUwaAoJQ/export?format=tsv), [Sheets](https://docs.google.com/spreadsheets/d/1ViO4swIovvuRJC-kiPaQOIdAkuwHCbQvTQlNUwaAoJQ/edit?usp=sharing)] Mitmekeelne süsteem sisendkõne kordamiseks Androidi kõnesüntesaatoriga (nt häälduse harjutamiseks). (Kui eesti keele süntesaatorit pole installeeritud, siis kasutatakse soome või hispaania keele oma.)
- [[TSV](https://docs.google.com/spreadsheets/d/1GvBl2Tq9sZRrQCRnsttpYliyR7vraDpMHReVyoOijq4/export?format=tsv), [Sheets](https://docs.google.com/spreadsheets/d/1GvBl2Tq9sZRrQCRnsttpYliyR7vraDpMHReVyoOijq4/edit?usp=sharing)] Lihtne rakendus kõnekorpuse kogumiseks, mis koosneb valdavalt sisendfraasidest, mille Kõnele palub kasutajal järjest ette lugeda.
- (_vajalik rakendus [ee.ioc.phon.android.speechtrigger](https://github.com/Kaljurand/speech-trigger)_) [[TSV](https://docs.google.com/spreadsheets/d/1jYhX5ARj_I5c78K9ECUDmE9gr96xes732vFlJsuGLtk/export?format=tsv), [Sheets](https://docs.google.com/spreadsheets/d/1jYhX5ARj_I5c78K9ECUDmE9gr96xes732vFlJsuGLtk/edit?usp=sharing)] Näide, kus Kõnele töötab kui kõnepõhine lüliti (st ootab kuni kasutaja lausub etteantud fraasi nn _wake up phrase_), mis lülitab sisse Hue valgustite käsustiku, pärast mille täitmist lülitirežiim jätkub.
- [[TSV](https://docs.google.com/spreadsheets/d/1ZrkBeDT3C9OplX4uDL_HG4lLAJajBgZDxy8VK_3JyYg/export?format=tsv), [Sheets](https://docs.google.com/spreadsheets/d/1ZrkBeDT3C9OplX4uDL_HG4lLAJajBgZDxy8VK_3JyYg/edit?usp=sharing)] Reeglid erinevate veasituatsioonide esilekutsumiseks (testimiseks).

### Reeglid kui liides dialoogisüsteemile

Olgugi, et ümberkirjutusreeglite abil saab luua lihtsamaid dialoogisüsteeme, on reaalsete süsteemide (allpool "robot") loomisel mõtekam kasutada siiski võimsamaid vahendeid loomuliku keele töötluseks ning suhtluseks teiste seadmetega. Sellisel juhul oleks Kõnele lihtsalt transkriptsiooniteenuse pakkuja, st robot ei peaks oskama ise kõne tuvastada.

Järgmine reegel (mille peaks salvestama reeglistikku nimega "Robot") saadab fraasiga "hei Robot" algava päringu edasi kohtvõrku installeeritud veebiliidesega robotile:

- __Utterance__ = `^hei Robot (.+)$`
- __Command__ = `activity`
- __Arg1__ =

{% highlight json %}
    {
      "component": "ee.ioc.phon.android.speak/.activity.FetchUrlActivity",
        "data": "http://192.168.0.11:8000/?lang=et-EE&q=$1",
        "extras": {
          "ee.ioc.phon.android.extra.RESULT_LAUNCH_AS_ACTIVITY": true
        }
    }
{% endhighlight %}

Nt kui kasutaja ütleb "hei Robot mängi Ivo Linnat", siis jõuab robotile päring "mängi Ivo Linnat", mida robot peab ise edasi analüüsima ja sellele seejärel kuidagi reageerima.

Juhul kui robot tahab küsida jätkuküsimusi, siis peaks ta päringule vastama umbes sellise JSON struktuuriga.

{% highlight json %}
{
"component": "ee.ioc.phon.android.speak/.activity.SpeechActionActivity",
"extras": {
    "ee.ioc.phon.android.extra.VOICE_PROMPT": "Mis laulu?",
    "android.speech.extra.MAX_RESULTS": 1,
    "android.speech.extra.LANGUAGE": "et-EE",
    "ee.ioc.phon.android.extra.AUTO_START": true,
    "ee.ioc.phon.android.extra.RESULT_UTTERANCE": "(.+)",
    "ee.ioc.phon.android.extra.RESULT_REPLACEMENT": "hei Robot $1",
    "ee.ioc.phon.android.extra.RESULT_REWRITES": ["Robot"]
  }
}
{% endhighlight %}

Kõnele komponent `FetchUrlActivity` käivitab sellise vastuse peale Kõnele otsingupaneeli, mis ütleb läbi Androidi kõnesüntesaatori "Mis laulu?", lindistab kasutaja kõnesisendi, ning lisab transkriptsioonile prefiksi "hei Robot", tagades nõnda, et tulemus saadetakse jälle roboti veebiliidesele.

Nõnda on võimalik pikem käed-vaba dialoog robotiga, kus Kõnele roll on olla lihtsalt kõnetuvastaja, ning muud ülesanded (nt loomuliku keele analüüs, eelneva dialoogi mäletamine, teadmised kasutaja profiilist, suhtlemine teiste seadmetega) on roboti kanda.

## Grammatikapõhine kõnetuvastus

(_Eeldab grammatikatoega teenuse kasutamist_)

Kõnele võimaldab igale Androidi rakendustele, mis on Kõnele vähemalt ühe korra välja kutsunud
omistada tõlkegrammatika.  Grammatika omistamisel rakendusele on sisuliselt kaks funktsiooni:

  - deklareerimine, et ainult teatud laused ja sõnavara omab vastava rakenduse kontekstis mõtet, nt mõõtühikute teisendamise rakendus võiks toetada fraase nagu "kaks meetrit jalgades" kuid peaks välistama fraasid nagu "mis ilm on tartus" või "kolm meetrit ruutmeetrites" (viimane kasutab küll õiget sõnavara, kuid teeb seda semantiliselt valel moel). Kui kõnetuvastusserverile sel viisil grammatika esitada, siis on väiksem tõenäosus, et tuvastamisel tehakse viga;
  - tuvastustulemuse "tõlkimine" kujule, mis sobib vastavale rakendusele paremini, nt mõõtühikute teisendamise rakendused eeldavad tüüpiliselt inglise keelest, numbritest ja SI ühikutest/prefiksitest koosnevat sisendit, st "convert 100 m/s to km/h", mitte eestikeelset "kui palju on sada meetrit sekundis kilomeetrites tunnis".

Sellised grammatikad ei kata loomulikku keelt (nt eesti keelt) tervikuna, vaid ainult selle
mingit piiratud alamosa. Nt lauseid mõõtühikute või valuutade teisendamise kohta,
aritmeetika avaldiste keelt, aadressipäringute keelt jne.

Iga grammatika on esitatud HTTP-veebiaadressina (nt
`http://kaljurand.github.com/Grammars/grammars/pgf/Action.pgf`),
mis tuleb eelnevalt serveris registreerida.
Kõnele seadetes, menüüs "Grammatikad" on loend juba registreeritud grammatikatest.
Grammatika omistamiseks rakendusele tuleb sellele "Rakendused" loendis pikalt vajutada (_long tap_).

Vaatleme näitena grammatikapõhist tuvastust rakenduse _Google Now_ otsingureal.
See rakendus võimaldab loomulikus keeles antud sisendi põhjal teha erinevaid toimingud
(äratuskella helisema panemine, aadressiotsing, mõõtühikute teisendamine, jms), kuid
eesti keelt sisendina ei toeta, st eestikeelse sisendi puhul sooritatakse pelgalt veebiotsing.
Sisendi saab anda otse kõne abil, kuid sel juhul oskab _Google Now_ kasutada ainult
Google'i kõnetuvastajat. Õnneks on sisend võimalik ka klaviatuurilt ning kasutada võib ükskõik
millist klaviatuuri, sh ka Kõnele kõneklaviatuuri.
Järgmised pildid näitavad valuuta konverteerimist, kus sisend
("kaksteist tuhat kolmsada nelikümmend viis norra krooni eurodes") jooksvalt _Google Now_-le
arusaadavale kujule ("convert 12345 NOK to EUR") teisendatakse.

<img title="Ekraanipilt: dikteerimine otsingureal" alt="Ekraanipilt: dikteerimine otsingureal." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-24-20-47-02.png">
<img title="Ekraanipilt: transkriptsioon on teisendatud formaalseks avaldiseks" alt="Ekraanipilt: transkriptsioon on teisendatud formaalseks avaldiseks." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-24-20-47-32.png">
<img title="Ekraanipilt: avaldisele vastav Google'i otsingutulemus" alt="Ekraanipilt: avaldisele vastav Google'i otsingutulemus." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-24-20-47-59.png">

Selleks, et see nii töötaks peab kõneklaviatuuril olema sisse lülitatud "eesti keel/grammatikatoega".
Samuti, peab _Google Now_ rakendusele ("com.google.android.googlequicksearchbox")
omistama _Action_-grammatika. (Seda omistust kasutab ainult grammatikatoega teenus,
muude teenuste puhul seda ignoreeritakse.)

<img title="Ekraanipilt: Kõnele seaded" alt="Ekraanipilt: Kõnele seaded." src="{{ site.baseurl }}/images/et/Screenshot_2015-09-24-20-49-21.png">
<img title="Ekraanipilt: Kõnele teenuste ja keelte valik" alt="Ekraanipilt: Kõnele teenuste ja keelte valik." src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-22-04-18.png">
<img title="Ekraanipilt: nimekiri rakendustest ja nendele vastavatest grammatikatest" alt="Ekraanipilt: nimekiri rakendustest ja nendele vastavatest grammatikatest." src="{{ site.baseurl }}/images/et/Screenshot_2015-06-13-21-49-06.png">

Olemasolevate grammatikate kohta on võimalik lugeda aadressil <http://kaljurand.github.io/Grammars/>
ning nende registreerimine ja kasutamine grammatikatoega serveris on kirjeldatud lehel
<http://bark.phon.ioc.ee/speech-api/v1>.
Vt ka rakendust [Arvutaja](http://kaljurand.github.io/Arvutaja/), mis kasutab Kõnelet grammatikatoega kõnetuvastajana.

## Kõnele nutikellal

Kõnele toimib peaaegu kogu oma võimaluste ulatuses ka nutikellal, kuigi selle kasutajaliidest
pole nutikella väiksele ekraanile veel kohandatud. Paigaldamist, seadistamist ja peamiseid
kasutusnäiteid on kirjeldatud (inglise keeles) siin:
<https://github.com/Kaljurand/K6nele/tree/master/docs/android_wear>.
(Hetkel pole võimalik Kõnelet otse Google Play poest kellale paigalda, selleks tuleb
kasutada ADB programmi.)

## Kõnele ja Android Things

Kõnele toimib eksperimentaalselt ka Android Things platvormil, vt
<https://github.com/Kaljurand/K6nele/tree/master/docs/android_things>.

## Veaolukorrad

Kõnele kasutamine ebaõnnestub järgmistes olukordades:

- võrguühendus serverini puudub või on liiga aeglane;
- server ei tööta või on ülekoormatud;
- rakendusel pole ligipääsu mikrofonile, sest mõni teine rakendus parasjagu lindistab.

Nendes olukordades väljastab Kõnele vastava veateate.
Muud sorti vead palun raporteerida aadressil <http://github.com/Kaljurand/K6nele/issues>
või kirjutades <mailto:kaljurand+k6nele@gmail.com>.

Tagasiside sellele juhendile palun jätta allolevasse kasti.
