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
transkribeerimiseks laaditakse serverisse umbes 1 MB jagu andmeid. Wifivõrkudes
on Kõnele kasutuskiirus tüüpiliselt oluliselt parem kui 3G jms võrkudes.

Järgnev juhend kirjeldab Kõnele seadistamist ja kasutamist eestikeelse kasutajaliidesega
Android v5 (Lollipop) ja uuemates seadmetes. Androidi kasutajaliidese tõlge ja struktuur on seadmeti ja versiooniti mõnevõrra erinev, kuid mitte oluliselt.

## Demovideod

- [Esimene video](https://www.youtube.com/watch?v=gKFIWSA2GWc) näitab
(1) kõnepõhist veebiotsingut;
(2) kõneklaviatuuri sisse lülitamist ja sellega kirja kirjutamist;
(3) aadressiotsingut kaardirakenduses;
(4) Kõnele konfigureerimist Androidi vaikimisi kõnetuvastajaks, ja selle kasutamist _Google Translate_ rakenduses;
(5) tõlkegrammatika omistamist _Wolfram Alpha_ rakendusele, ja selles mõõtühikuteisendamist;
(6) _Arvutaja_ rakenduse kasutamist (vahelduseks inglise keeles) äratuskella seadmiseks.
Video on tehtud Kõnele versiooniga 0.8.56, uuemates versioonides on kasutajaliides natuke
muutunud.
(Samuti, _Arvutaja_ rakendust toetav teenus on praeguseks aegunud ning pole sellisel kujul enam kasutatav.)

- [Teine video](http://www.youtube.com/watch?v=VLjV8JulEow) näitab Kõnele paigaldamist Android v11 nutitelefoni, kus rakendusele
mikrofoni kasutamise õiguse andmine on keerukam, ning kõnetuvastuskomponent tuleb
paigaldada eraldi rakendusena
[Kõnele service](https://github.com/Kaljurand/K6nele-service).
Video on tehtud Kõnele
beetaversiooniga v1.7.42, teistes versioonides on kasutajaliides natukene
teistsugune.

- [Kolmas video](https://youtu.be/PWngf5onMaE) näitab Kõnele v1.7.xx lisatud nuppude funktsionaalsust:
(1) mikrofoninupu svapidele saab ümberkirjutusreeglite läbi käske omistada;
(2) sisselülitatud ümberkirjutusreeglid kuvatakse nuppudena, ja nii saab näiteks ise teha kalkulaatorirakenduse;
(3) tekstikastis kopeeritud tekst salvestub tabelisse nimega "#c" (_clipboard_), ja nii saab hiljem pikemaid ja/või korduvaid tekste ühe nupuvajutusega taassisestada.

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

<img style="float: right" title="Ekraanipilt: seadistamine" alt="Ekraanipilt: seadistamine." src="{{ site.baseurl }}/images/et/Screenshot_2018-12-27-09-29-43.jpg">

Sama mikrofoninupuga paneeli ülemises paremas nurgas on nupp, mis viib Kõnele seadetesse.
Need võimaldavad Kõnele töökäiku erinevatel viisidel suunata, määrates nt

- milliseid tuvastusteenusi ja -keeli kasutatakse;
- kas/kuidas rakendada tuvastustulemusele tõlkegrammatikaid ja ümberkirjutusreegleid (vt allpool);
- kas lindistamine algab automaatselt või peale nupule vajutamist;
- kas lindistamine lõpeb kui sisendkõnesse tekib paus, või alles siis, kui nupule on vajutatud;
- kas lindistamise algusest ja lõpust teavitatakse lühikese helisignaaliga.

Mõned nendest seadetest puudutavad ainult Kõnele enda kasutajaliidest (st klaviatuuri ja otsingupaneeli) ning
seega ei rakendu juhul kui Kõnelet kasutatakse läbi teise rakenduse.

Kõnele toetab kahte erinevat kõnetuvastusteenust:

  - "grammatikatoega" teenus (kasutab serverit tarkvaraga <http://github.com/alumae/ruby-pocketsphinx-server>)
    lubab sisendkõnele omistada tõlkegrammatikaid, kuid on aeglasem ja sisendkõne pikkus ei tohi ületada
    30 sekundit;
  - "kiire tuvastusega" teenus (kasutab serverit tarkvaraga <http://github.com/alumae/kaldi-gstreamer-server>)
    tagastab tuvastustulemuse juba rääkimise ajal, ega sea sisendkõne pikkusele mingit piirangut.

Mõlema teenuse tarkvara on vaba lähtekoodiga ja teenuse veebiaadressid on Kõneles konfigureeritavad. Seega võib teenuse installeerida suurema kiiruse ja privaatsuse huvides kohtvõrku. Seda käsitleb pikemalt [eraldi peatükk](#tuvastusserver-koduvõrgus).

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

Otselink on lihtne näide, kuidas käivitada Kõnele otsingupaneel sisendparameetritega, mille väärtused erinevad nendest, mis seadetes kirjas. Kõnele toetab palju erinevaid sisendparameeterid (nn EXTRA), mis võimaldavad teistel rakendustel (nt Tasker, vt [eraldi peatükk](#kõnele-ja-tasker)) Kõnelega otse suhelda. Toetatud EXTRAte kohta vt täpsemalt [Developer's Guide]({{ site.baseurl }}/docs/en/developer_guide.html).

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

<img title="Ekraanipilt: otselink klaviatuuri sisselülitamise menüüsse" alt="Ekraanipilt: otselink klaviatuuri sisselülitamise menüüsse." src="{{ site.baseurl }}/images/et/Screenshot_20181227-093420.png">

Androidi seadete hierarhias asub vastav menüü üsna sügaval, ning lisaks erineb selle asukoht
Androidi versiooniti:

- v5: `Keeled ja sisestamine -> Klaviatuur ja sisestusmeetodid`
- v6-?: `Täpsemad seaded -> Keel ja klahvistik -> Virtuaalne klaviatuur`
- v11: `Süsteem -> Keeled ja sisend -> Ekraanil kuvatav klaviatuur -> Ekraanil kuvatavate klaviatuuride haldamine`

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

<img title="Ekraanipilt: traditsiooniline klaviatuur katab pool märkmerakendusest" alt="Ekraanipilt: traditsiooniline klaviatuur katab pool märkmerakendusest." src="{{ site.baseurl }}/images/et/Screenshot_20201230-114003.png">&harr;<img title="Ekraanipilt: Kõnele katab ainult kolmandiku märkmerakendusest." src="{{ site.baseurl }}/images/et/Screenshot_20201230-114031.png">

Traditsiooniline klaviatuur katab pool märkmerakendusest. Kõnele katab ainult veerandi.


### Omadused

Lisaks mikrofoninupule, mis käivitab/lõpetab/katkestab kõnetuvastuse, toetab Kõnele
klaviatuuri puutetundlik paneel erinevaid operatsioone, sõltuvalt
klaviatuuriseadetest,
tekstivälja tüübist, ja sellest, kas paneel on tuvastusrežiimis või mitte.
Osa nendest operatsioonidest on võimalik
[tekstitoimetuskäskude](#tekstitoimetuskäsud) abil dubleerida.

Versioonis 1.7 toetab mikrofoninupp lisaks svaipimist ja pikalt/topelt vajutamist, mida
saab kasutajadefineertud operatsioonidega siduda,
vt [Lausung kui nupuvajutus](#lausung-kui-nupuvajutus).

#### v1.7

- vasak ülemine nurk, klaviatuurinupp:

  - lühike vajutus vahetab eelmisele klaviatuurile,

  - pikk vajutus vahetab järgmisele klaviatuurile;

- parem ülemine nurk, tekstivälja tüübist sõltuv "action" nupp, nt

  - otsinguväli: lühike vajutus sooritab otsingu,

  - üherealine tekstiväli (nt pealkirjaväli): lühike vajutus liigutab kursori järgmisele väljale,

  - tavaline mitmerealine tekstiväli: lühike vajutus lisab reavahetuse;

- parem alumine nurk:

  - lühike vajutus muudab paneeli tüüpi,

    1. mikrofoni- ja kustutamisnupuga paneel
       (kustutamisnupp kustutab kursorist vasakul oleva sümboli või praeguse tekstivaliku),

    2. lausunginuppudega paneel, vt [Lausung kui nupuvajutus](#lausung-kui-nupuvajutus),
       <img style="float: right" title="Ekraanipilt: lausunginuppudega paneel" alt="Ekraanipilt: lausunginuppudega paneel." src="{{ site.baseurl }}/images/et/Screenshot_20201230-115204.png">

    3. ainult nurganuppudega (ja seega väiksem) paneel,
       <img style="float: right" title="Ekraanipilt: ainult nurganupudega paneel" alt="Ekraanipilt: ainult nurganupudega paneel." src="{{ site.baseurl }}/images/et/Screenshot_20201230-114051.png">

  - pikk vajutus käivitab/lõpetab/katkestab kõnetuvastuse (samamoodi nagu mikrofoninupp);

- vasak alumine nurk, keele/teenuse vahetamise nupp (kui mitu keelt/teenust on aktiivsed):

  - lühike vajus lülitab järgmisele keelele/teenusele,

  - pikk vajutus kuvab kõik võimalikud keeled/teenused, võimaldades valikut muuta;

- paneel:

  - lühike vajutus tühistab praeguse tekstivaliku,

  - topeltvajutus lisab tühiku,

  - svaip vasakule liigutab kursori vasakule (svaip vasakule üles teeb sama kiiremini),

  - svaip paremale liigutab kursori paremale (svaip paremale alla teeb sama kiiremini),

  - pikk vajutus valib kursori all/kõrval oleva sõna ning siseneb valiku-režiimi, kus svaibid muudavad valiku ulatust,

  - pidev vajutus klaviatuuri vasakule äärele liigutab kursorit vasakule,

  - pidev vajutus klaviatuuri paremale äärele liigutab kursorit paremale.


#### v1.6

- vasak ülemine nurk:

  - klaviatuuri-ikoon, kui tuvastust ei toimu:
  lühike vajutus vahetab eelmisele klaviatuurile,
  pikk vajutus vahetab järgmisele klaviatuurile;

  - noole-ikoon, tuvastuse ajal:
  lühike vajutus muudab klaviatuuri paneeli väikseks, või tagasi suureks;

- parem ülemine nurk, tekstivälja tüübist sõltuv "action" nupp, nt

  - otsinguväli: lühike vajutus sooritab otsingu,

  - üherealine tekstiväli (nt pealkirjaväli): lühike vajutus liigutab kursori järgmisele väljale,

  - tavaline mitmerealine tekstiväli: lühike vajutus lisab reavahetuse;

- paneel, variant 1:

  - topeltvajutus lisab tühiku,
  - svaip vasakule kustutab kursorist vasakul asuva sõna,
  - svaip paremale lisab reavahetuse,
  - pikk vajutus valib kogu teksti;

- paneel, variant 2 (vaikimisi, aga seadetes muudetav):

  - topeltvajutus lisab tühiku,
  - kustutamise ikoon kustutab kursorist vasakul oleva sümboli või praeguse valiku,
  - svaip vasakule liigutab kursori vasakule (svaip vasakule üles teeb sama kiiremini),
  - svaip paremale liigutab kursori paremale (svaip paremale alla teeb sama kiiremini),
  - pikk vajutus valib kursori all/kõrval oleva sõna ning siseneb valiku-režiimi, kus svaibid muudavad valiku ulatust,
  - pidev vajutus klaviatuuri vasakule äärele liigutab kursorit vasakule,
  - pidev vajutus klaviatuuri paremale äärele liigutab kursorit paremale.

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
Android v6-7: `Seaded -> Rakendused -> Rakenduste seadistamine (hammasrattaikoon) -> Vaikerakendused -> Abirakendus ja häälsisend -> Häälsisend`; Kõnele seadetes on ka otsetee `Kõnetuvastusteenused (süsteemsed seaded)`.)


<img title="Ekraanipilt: nimekiri paigaldatud tuvastusteenustest" alt="Ekraanipilt: nimekiri paigaldatud tuvastusteenustest." src="{{ site.baseurl }}/images/et/Screenshot_2014-12-23-21-16-00.png">

Ühe Kõnele teenustest võib seada vaikimisi teenuseks. See ei garanteeri küll kahjuks, et kõik rakendused
hakkavad nüüd kõnetuvastuseks Kõnelet kasutama, sest paljud neist ignoreerivad kasutajamääratud
vaikeväärtust ja kasutavad endiselt nt Google'i teenust.
Selline on olukord paljude klaviatuurirakendustega, milles oleva mikrofoninupu vajutamine
käivitab Google'i kõnetuvastusteenuse, ning seda muuta pole võimalik. Üheks erandiks
on _SlideIT Keyboard_, mida saab konfigureerida Kõnelet kasutama.
Hea ülevaade parimatest klaviatuurirakendustest (eesti keeles kirjutamise
seisukohast) on ajakirjas [[digi] 5/2014](http://www.digi.ee/2014/05/arhiiv/).

<del>Huvitav olukord on Google'i tõlkerakendusega (_Google Translate_), mis kasutab kõnetuvastuseks
üldiselt Google'i tuvastajat, kuid keelte jaoks, mida see ei toeta (nt eesti keel)
kasutab vaikimisi määratud kõnetuvastusteenust. Seega saab Kõnele ja Google'i tõlkerakendusega
teha kõnest-kõnesse tõlget eesti keelest paljudesse teistesse keeltesse.</del>
(Alates dets. 2018 enam ei toimi, sest Google (arvab, et) oskab ise eestikeelset kõne tuvastada, ning
Translate pole seega teistele tuvastajatele avatud.
Lahenduseks võib olla Google'i tuvastaja telefonist eemaldamine.
Samuti toimib endiselt teksti sisestamine Kõnele klaviatuuri abil.)

Android v6 on lisanud nn "Abirakenduse" mõiste, kuid kui abirakenduseks on valitud _Google'i rakendus_, siis valikut `Häälsisend` ei kuvata, ning Kõnele rakendust vaikimisi teenuseks seada ei saa. Üheks kahetsusväärseks tagajärjeks on see, et nüüd puudub võimalus korraga kasutada funktsionaalsust "Google Assistant" ja eestikeelset kõnesisendit rakenduses _Google Translate_.
(Vt ka vearaporteid [200494](https://code.google.com/p/android/issues/detail?id=200494)
ja [200496](https://code.google.com/p/android/issues/detail?id=200496).)

## Ümberkirjutusreeglid

Ümberkirjutusreeglid on Kõnele kasutaja poolt loodavad reeglid tuvastusteenuse poolt tagastatud transkriptsiooni jooksvaks muutmiseks, ja sellele käskude rakendamiseks. Vaata/paigalda [ümberkirjutusreegleid](rewrites.html).

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
Veergude järjekord pole oluline. Kohustuslik veerg on ainult __Utterance__. Kui __Replacement__ puudub, siis on asendustekst alati tühisõne. Veerud __Locale__, __Service__ ja __App__ määravad, millise keele, tuvastusteenuse, ja rakenduse puhul on reegel aktiivne. Kõik regulaaravaldised on [Java regulaaravaldised](https://docs.oracle.com/javase/tutorial/essential/regex/). Põhjalik regulaaravaldiste õpetus on nt <http://www.regular-expressions.info>.

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
Need võimaldavad toimetada juba olemasolevat teksti käed vabalt (st ainult kõne abil), nt kursori liigutamist teksti sees ja väljade vahel (nt `selectReBefore`, `keyUp`, `imeActionNext`), sõnade/lausete valimist ja asendamist (nt `select`, `selectReAfter`, `replace`), operatsioone valikuga (nt `replaceSel`), lõika/kleebi/kopeeri operatsioone, [Androidi IME käske](https://developer.android.com/reference/android/view/inputmethod/EditorInfo.html) (nt `imeActionSend`). Enamikku käskudest on võimalik tagasi võtta (`undo`), mitu korda rakendada (`apply`), ja isegi kombineerida (`combine`). Argumendid võivad viidata parasjagu aktiivse valiku sisule funktsiooniga `@sel()`. Kursoriliigutamiskäskude puhul, mille argumendiks on regulaaravaldis (`..Re..`), määrab selle esimene alamgrupp kursori uue asukoha.
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
- __Command__ = `selectReAfter`
- __Arg1__ = `[.?!]\\s*()[^.?!]+[.?!]`
- __Arg2__ = `2`

### Lausung kui nupuvajutus

(_Alates Kõnele v1.7.28_)

Mõningaid toimingud on mõistlikum kõneliidese asemel nuppudele vajutades läbi viia (klaveri mängimine, liftis korruse valimine, jms).
Lisaks eelkirjeldatud nuppudele ("action" nupp, kustutamisnupp, ...) ja kursori liigutamisele Kõnele paneelil, toetab
Kõnele puutetundlikust veel kahel moel, mis on mõlemad tihedalt reeglistikega seotud.

Esiteks genereerib Kõnele mikrofoninupp lausungeid kujul nt `K6_Y_BTN_MIC_RIGHT`, kui seda svaipida (`UP`, `DOWN`, `LEFT`, `RIGHT`),
kahekordselt vajutada (`DOUBLETAP`), või pikalt vajutada (`LONGPRESS`).
Samuti sõltub genereeritud lausung nupu olekust: kollane (`Y`) või punane (`R`). Reeglid võimaldavad (juba eelkirjeldatud moel)
siduda käske selliste mikrofoninupupuudutustega.

Näide. Paremele svaip postitab sõnumirakenduses tekstiväljal parasjagu oleva teksti.

- __App__ = `(talk|fireball|teams)`
- __Utterance__ = `^K6_._BTN_MIC_RIGHT$`
- __Command__ = `imeActionSend`

Teiseks on võimalik kõiki aktiivseid reegleid nuppudena kuvada. Ühest küljest annab see parema
ülevaate, millised reeglid on antud kontekstis (rakenduses, keeles, teenuses) aktiivsed. Kuid
lisaks saab nüüd reegleid nupuvajutusega käivitada. Ning võib disainida reeglistikke, mida polegi
plaanis kõne abil käivitada (PIN-koodi sisestamispaneel, lemmik emotikonid, kalkulaator, jms).
Lisaveerg __Label__ määrab nupu ikooni.

Näide. Nupp emotikoni sisestamiseks.

- __Utterance__ = `^button_001$`
- __Command__ = `replaceSel`
- __Arg1__ = `🙂`
- __Label__ = `🙂`

Nupud laotakse ekraanile kolmes veerus ja iga reeglistik on eraldi _tabis_. Allolev ekraanipilt näitab võimalikku
kalkulaatoridisaini.

<img title="Ekraanipilt: klahvistik Kalkulaator" alt="Ekraanipilt: klahvistik Kalkulaator." src="{{ site.baseurl }}/images/et/Screenshot_20200612-012835.png">

### Reeglite tegemine ja paigaldamine

Reeglifaili loomiseks ja salvestamiseks sobib iga tabelarvutusprogramm. Nt [Google'i Arvutustabelid](https://www.google.com/intl/et/sheets/about/) (_Google Sheets_) võimaldab selliseid tabeleid luua nii lauaarvutis kui ka mobiiliseadmes, ning siis erinevate seadmete ja kasutajate vahel TSV-kujul jagada. Faili laadimiseks Kõnele rakendusse on erinevaid võimalusi:

- Kõnele menüüvalik "Ümberkirjutusreeglid" avab nimekirja olemasolevatest reeglistikest. Seal on Lisa-nupp (plussmärk ringi sees), mis avab failibrauseri, mille abil tuleb soovitava TSV-faili juurde navigeerida ning sellele klikkida. Reeglifail peab sel juhul seadme failide hulgas juba olema.
- Veebibrauseris klikkida TSV-laiendiga veebilingile (proovi nt [seda linki]({{ site.baseurl }}/rewrites/tsv/k6_skill_map.tsv)), mille tulemusena fail seadmesse laaditakse, ja avaneb võimalus sellele klikkida ja see Kõneles avada. See protsess on erinevates brauserites mõnevõrra erinev.
- Veebibrauseris klikkida k6-prefiksiga lingile (proovi nt PAIGALDA-linki [sellel lehel]({{ site.baseurl }}/docs/et/1liMiWDiU4iN1faAENtAIbFenbtpjKocJvNxjyuW9hqU.html)). Sel juhul on kogu tabel salvestatud lingi sisse ning veebibrauser annab selle klikkimisel kohe edasi Kõnelele. Paigaldamiseks on see viis kõige lihtsam, aga reeglifaili loomisel lisandub k6-lingiks tegemise samm (nt Pythonis: ``'k6://' + base64.urlsafe_b64encode( faili_sisu )``).
- Juhul kui tabelarvutusrakenduses (Google Sheets, Microsoft Excel, ...) on failijagamismenüü, kus saab tabeli eelnevalt teisendada TSV-kujule (kõigis kahjuks pole), siis saab tulemuse otse jagada Kõnelega. See on kõige lihtsam viis, juhul kui reegleid on vaja pidevalt (endal) muuta.
- Kõnele seadete muutmine Kõnele alamrakendusega ``GetPutPreferenceActivity``, nt ADB abil (vt näidet [siin](https://github.com/Kaljurand/K6nele/tree/master/docs)). See sobib olukordadesse, kus on vaja paigaldada/uuendada korraga mitut reeglifaili. Samuti on see hetkel ainuvõimalik meetod nutikellal ja Android Things seadmetel.

Reeglifaili kasutamiseks tuleb see eelnevalt aktiveerida. Kui mitu faili on aktiivsed, siis neid rakendatakse tähestikujärjekorras.

Järgnevad ekraanipildid näitavad faili jagamist rakenduses Google'i Arvutustabelid, menüüde "Jagamine ja eksportimine" ja "Saada koopia" abil.

<img title="Ekraanipilt: ümberkirjutusreeglid tabelarvutusrakenduses" alt="Ekraanipilt: ümberkirjutusreeglid tabelarvutusrakenduses." src="{{ site.baseurl }}/images/et/Screenshot_20160925-202955.png">
<img title="Ekraanipilt: ümberkirjutusreeglite jagamine menüüvalikuga 'Jagamine ja eksportimine'" alt="Ekraanipilt: ümberkirjutusreeglite jagamine menüüvalikuga 'Jagamine ja eksportimine'." src="{{ site.baseurl }}/images/et/Screenshot_20160925-203014.png">
<img title="Ekraanipilt: ümberkirjutusreeglite jagamine menüüvalikuga 'Saada koopia'" alt="Ekraanipilt: ümberkirjutusreeglite jagamine menüüvalikuga 'Saada koopia'." src="{{ site.baseurl }}/images/et/Screenshot_20160925-203027.png">
<img title="Ekraanipilt: ümberkirjutusreeglite teisendamine TSV-formaati" alt="Ekraanipilt: ümberkirjutusreeglite teisendamine TSV-formaati." src="{{ site.baseurl }}/images/et/Screenshot_20160925-203041.png">
<img title="Ekraanipilt: ümberkirjutusreeglite importimine Kõnele rakendusse" alt="Ekraanipilt: ümberkirjutusreeglite importimine Kõnele rakendusse." src="{{ site.baseurl }}/images/et/Screenshot_20170115-154706.png">
<img title="Ekraanipilt: imporditud ümberkirjutusreeglite nimekiri" alt="Ekraanipilt: imporditud ümberkirjutusreeglite nimekiri." src="{{ site.baseurl }}/images/et/Screenshot_20170115-160910.png">

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

<img style="float: right" title="Ekraanipilt: Kõnele nutikella klaviatuurina" alt="Ekraanipilt: Kõnele nutikella klaviatuurina." src="{{ site.baseurl }}/images/et/Screenshot_20181007.png">

(_Eksperimentaalne_)

Kõnele toimib peaaegu kogu oma võimaluste ulatuses ka nutikellal, kuigi selle kasutajaliidest
pole nutikella väiksele ekraanile veel kohandatud. Paigaldamist, seadistamist ja peamiseid
kasutusnäiteid kirjeldab (inglise keeles)

<https://github.com/Kaljurand/K6nele/tree/master/docs/android_wear>.

(Hetkel pole võimalik Kõnelet otse Google Play poest kellale paigalda, selleks tuleb
kasutada ADB programmi.)

## Kõnele ja Android Things

<a href="{{ site.baseurl }}/images/et/20190209_android_things.jpg">
<img style="float: right" title="Ekraanipilt: Kõnele ja Android Things, riistvara: Raspberry Pi, mikrofon, kõlar, aku, monitor (valikuline)" alt="Ekraanipilt: riistvara: Raspberry Pi, mikrofon, kõlar, monitor (valikuline)" src="{{ site.baseurl }}/images/et/20190209_android_things_small.jpg">
</a>

(_Eksperimentaalne_)

Kõnele toimib ka Android Things platvormil, nt Raspberry Pi riistvaral.
Paigaldamist, seadistamist ja peamiseid
kasutusnäiteid kirjeldab (inglise keeles)

<https://github.com/Kaljurand/K6nele/tree/master/docs/android_things>.

## Kõnele ja Tasker

(_Eksperimentaalne_)

Rakendus [Tasker](https://tasker.joaoapps.com/) võimaldab erinevaid nutiseadmetoiminguid automatiseerida.
Järgnevalt mõned Taskeri käsud (nn "task"), mis käivitavad Kõnele otsingupaneeli, kuid defineerivad
üle mõned selle sisendparameetritest (nn EXTRA). Käskude importimiseks Taskerisse (vajalik v5.5+)
tuleb klikkida allolevatele linkidele.

- [K6 autostart](taskertask://H4sIAAAAAAAAAIVTy26DMBA8J18R+QNsHgGSyliKlEuUQ6UmPSODXWKFAHKM2/59/SAtapv2gpeZ2Z1lzeIjvZ653FJFF1eZA7BgWuQgBAulc5DACJYvKSDzGbZCJ1EmiCw0wxWjipMwiYN1GkXZar2MMfKgpfmEjpN1skyXGPFPWjASYWSe9qW9cLJPCzqo7qqoVBhZxDK9FCQMAoxsYIG9YK4Rc7o2ZrihQ1uddltb7zN2TH+uCSs5ZL2GfVOaIgZwjLb1dYiRvhmh/djLplKia50HrVQAFprnIBu9qo5xssoy86E2cthBSa+W9aiOAfLUrlU3ygxV0yYHwY2bpEXf0iZUfJ9a3ijCORRdBftT10LaMtkJBvmbkhRuno+PxeG4eTo+KDlwjEz+j0LJfY/0PpX9aX/tOT3/brf6PxGayQst1Ds89JxXJ38nmxGclJ0MeD0OOPS9YuSTXLy71E4mqtabR+N92tu/vBbUSYtG1CdVDk1ZdINqRMu//kNkSthFQHYTyNyffnXI/APM0lWWSAMAAA==) käivitab Kõnele EXTRAga `AUTO_START=true`, st sisendkõne lindistamine algab automaatselt.
- [K6 send](taskertask://H4sIAAAAAAAAAI1T266bMBB8Tr4i8gfYQA6QRMYS0slDlFaVAkftG3JsN7ESDCIObf++vkAOatPLC17P7MyuWRuX9HYR3SvVdHHrMgAWvJcZCMFC9xmIYQSPXxNA5jNsE12KNkFsoRlmnGpBwngZrJMoSlfrlyVGHrS0mNBJvIoiS4sHLTmJMTJfu1G1IPukugnFMbIbC7adJGEQYGQDC+wldz2Y1XUww1d6V+y8eyURRo/YMe3lRPhRQN72sL0ejYkBHNNb/z7EqB8Lof3QRs60bJSrQZkOwKIXGUiHWqzhgqzS1JzRRg4rdOezu9OQvQTIUzulR8r8z55eMxCM3EQW/SKbUMs/Uy8jRYSAsmGwPTcKUsW7RnIovuuOwvyt/FQVZX4oN7q7C4yM/jej+D+MDtvi7UNZHbafD7tyW2wKN6VnbsnDbXS4tUKw82D0Mf9SebNiEz53SP/aj3Gjl+fC1b+F0MxU9lL/gIXryk87H8CJ7WR062F0oR8CRl7k4l19cmmSKV88Gm6KvVf1t4o1dX1XklGrqERN5fX9ciOjtg8L2ZdF5n71T5HMfwI2fbaomAMAAA==) käivitab Kõnele EXTRAtega `AUTO_START=true`, `MAX_RESULTS=1`, `RESULT_REWRITES="Send"`, st lindistamine algab automaatselt, tagastatakse ainult üks transkriptsioon, ning sellele rakendatakse ümberkirjutusreeglistikku "Send".
- [K6 lights](taskertask://H4sIAAAAAAAAAKVX3W7aMBS+bp8C5aJXm/ODaKkaIgWatai0QUno2qvIEEO9hgQlDh0ST7EXmPaGe4TZcWgZLYlDbyA+Puf7PtvHP0f3YPqMkktIYCNNOpLUCJa4I6lSgyw7UgtoYDw9lYzjI5055i6EfmjMdKRPAkiQobaaqtJW2u2meq7pMjeybvTarapnqqKdarQbvXbjwKBt+ssa0RwZN6d+iGdPJNVl1mTmRYINVVF0mX0wgzkhOI5yIXBCFKmxRB3pLJdD9cQB5dPO1baqtlstqoUZ8q5uFgUh4nHJTOEBR/o9DNPcuIRhYWMwczCGYQABjgiKCBsxQD9JAoHZ8/r2ncGQq3xEwb6S1QIZP+ASghBGM+CSBEczAQYeWE3Tu7Zt13KMKR0rKsXdeApj7mrvxnGIYCRCIqx+YLqugRDA8QQsnuIIwChIYhyAdIHgM6BZgJeYrIC7QGjyxPPDLIzlQnJkQQUHrNJWXDWJ9eA5pmuchOSCQ5tJAlcDnBK+L09m5IJ1+jBJwpT4lVAKi+ja9mC9/mjyikQaebbveqbjrdckyRBjkOtR1BKlsoj+HWXbWkS6aoXTrfngO5Y7Gnjueq3WE6PWFqOxCNdz+ndXvuk45mPZTHFZVN13p+9ZVN4gP6m+8D9wXXfutNpym29y90zf0LFvh3Ru//7+9aeemuZGjbyTfMxemuRF3oom+NY2yggOwSuRAInoVqL55d9bjkuPSL9nX1pG87wU/Z1/fYrd06FPA2YoqUUrOjx7aDlmfsMopfhvfjUwDxnITrAA28gbjjyB+6hwFEU85DbajqzmGZq9G/PK2n8dlXJtooV5Drh0/ousJqJH/5XlVWRS4SSKdkgObUdu8ZCXeBonZDWNswTOQRhPYIiK6CKyOxg5XcNFUdAgcaNiYYTxDtBQtlj1MAryCBEQUIgQp/SI34yG5A92HpSCe9Ppm92BRS+m4cDsWf6N9ega1fnRqHq3lDnw45g+0g8W+NkR7pvrTwKKyEqz8TivJQKDvZgqOLe864HvPctqxfMiR2ZVDi+DZF4H8QadtU1BpPJCqil9mDs6exa8i9FKY0CGwTecILYz+7l1C4UaNihNigLDjqRIcl7gyfwFn3/357PcDU8izqUVhR4rD+cvPsxdedE4zsKxH2ckxBF6qx9lCsGqV5mVr8Yx/+f1rnH8D2AmLBr9DgAA) käivitab Kõnele EXTRAtega `AUTO_START=true`, `MAX_RESULTS=1`, `RESULT_REWRITES=["Lights","Lights.Hue"]` ja `PROMPT="💡"`, kuid vajab lisaks Taskerile selle pluginit IntentTask, et kasutada rohkem kui 3 EXTRAt ja mitut ümberkirjutustabelit.

(Selliste, taskertask-protokolliga linkide klikkimiseks e-kirjas või sotsiaalmeedias võib olla vajalik
teisendada nad kujule, mis algab protokolliga "http(s)",
nt <https://tinyurl.com/y773oej9>.)

Käskude käivitamiseks on mitmeid võimalusi, nt võib Taskeris defineerida
käsu käivitamistingimused (nn "profile") või
[teisendada käsk Androidi rakenduseks](http://tasker.dinglisch.net/userguide/en/appcreation.html), või
käivitada see lihtsalt Taskeri kasutajaliidese vahendusel.

## Tuvastusserver koduvõrgus

Tuvastusserveri kasutamine koduserveris lisab kogu süsteemile privaatsust
(sest audio ja selle transkriptsioon ei lahku koduvõrgust) ning võibolla ka
kiirust (sõltuvalt koduserveri kiirusest ja välisinterneti aeglusest).

Kõnetuvastusserveritarkvara <https://github.com/alumae/kaldi-gstreamer-server>
koos eesti keele mudelite ja käivitusskriptiga on saadaval Dockeri konteinerina
[alumae/docker-konele](https://hub.docker.com/r/alumae/docker-konele/), mis
teeb serveri jooksutamise koduarvutis ülilihtsaks.
See konteiner toetab nii "grammatikatoega" teenuse HTTP-liidest
kui ka "kiire tuvastusega" teenuse WebSocket-liidest (esimesel juhul küll GF grammatikaid
tegelikult toetamata).

Alustuseks on vaja ~3 GB kõvakettaruumi
ning Dockeri infrastruktuuri, mille paigaldamisjuhend nt Ubuntu Linuxile on
<https://docs.docker.com/install/linux/docker-ce/ubuntu/>.
Seejärel saab teenuse paigaldada käsuga

{% highlight sh %}
$ docker pull alumae/docker-konele
{% endhighlight %}

Käsu täitmine võtab mõnevõrra aega, sõltuvalt internetiühenduse ja arvuti kiirusest.

Teenuse käivitamiseks pordil 8080 (kasutada võib ka mõnd muud porti) tuleb
anda käsk

{% highlight sh %}
$ docker run -p 8080:80 -e num_workers=1 alumae/docker-konele
{% endhighlight %}

Jooksva teenuse testimiseks võib nt `curl` programmiga laadida sellesse
mõne audiofaili:

{% highlight sh %}
$ curl -T lause.ogg http://localhost:8080/client/dynamic/recognize
{"status": 0, "hypotheses": [{"utterance": "see on mingi suvaline lause"}], "id": "265...fea"}
{% endhighlight %}

Teenuse logi seire:

{% highlight sh %}
# Uuri välja konteineri nimi ja sisene sellesse:
$ docker ps
$ docker exec -it <nimi> bash

# Seal jälgi kahte logifaili:
tail -f master.log worker.log
{% endhighlight %}

Käivitatud teenuse kasutamiseks Kõnele rakenduses tuleb menüüs "Kõnetuvastusteenused"
ära muuta üks või mõlemad kaks serveriaadressi.
Teenuste aadressid sõltuvad koduserveri IP aadressist kohtvõrgus.
Nt juhul, kui serveriaadress on `192.168.0.38`
ja teenus sai käivitatud pordil `8080` tuleb Kõnele teenuste aadressid
muuta kujule:

- "grammatikatoega": `http://192.168.0.38:8080/client/dynamic/recognize`
- "kiire tuvastusega": `ws://192.168.0.38:8080/client/ws/speech`

Koduserveri IP aadressi teadasaamiseks võib
külastada koduruuteri konfigureerimislehekülge, tihti aadressil
<http://192.168.0.1>. Samuti on Kõnele menüüs WebSocket-aadress (alates Kõnele v1.6.84)
võimalik otsida nutiseadmega samas võrgus olevaid seadmeid,
ning kontrollida, kas neis jookseb kaldi-gstreamer-server. Nt teade
"2 vaba slotti" teenuse aadressikasti all tähendab,
et teenus töötab ning võimaldab hetkel maksimaalselt kahte samaaegset tuvastussessiooni.

<img title="Ekraanipilt: tuvastusserveri aadressi määramine" alt="Ekraanipilt: tuvastusserveri aadressi määramine." src="{{ site.baseurl }}/images/et/Screenshot_20180915-223504.png">

Käivitades Kõnele läbi teise rakenduse (nt Tasker, Android Debug Bridge, omatehtud
rakendus, või Kõnele ümberkirjutusreeglid), saab serveriaadressi üle
defineerida ka EXTRAga `ee.ioc.phon.android.extra.SERVER_URL`.

## Veaolukorrad

Kõnele kasutamine ebaõnnestub järgmistes olukordades:

- võrguühendus serverini puudub või on liiga aeglane;
- server ei tööta või on ülekoormatud;
- rakendusel pole ligipääsu mikrofonile, sest mõni teine rakendus parasjagu lindistab.

Nendes olukordades väljastab Kõnele vastava veateate.
Muud sorti vead palun raporteerida aadressil <http://github.com/Kaljurand/K6nele/issues>
või kirjutades <mailto:kaljurand+k6nele@gmail.com>.

Tagasiside sellele juhendile palun jätta allolevasse kasti.
