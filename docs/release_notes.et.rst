Versiooniajalugu
================

v0.8.5x
-------

1. Lisati uus kõnetuvastusteenus, mis kasutab kiire tagasisidega tuvastusserverit
(https://github.com/alumae/kaldi-gstreamer-server),
tänu millele saadetakse transkriptsioon
rakendusele juba rääkimise ajal ning tuvastustäpsus on oluliselt parem. (Hetkel puudub
serveril grammatikatugi, mistõttu toetab Kõnele ka varasemat grammatikatoega serverit.)
Juhul kui see teenus on määratud kogu seadmes vaikimisi kõnetuvastusteenuseks
``Seaded -> Keeled ja sisestamine -> Kõne -> Häälsisend``, siis on võimalik seda kasutada
ka teistes rakendustes, nt Google'i tõlkerakendus (Google Translate) kasutab
vaikimisi määratud kõnetuvastusteenust keelte jaoks, mida ta ise ei toeta (nt eesti keel).

2. Lisati kõneklaviatuur (nn "input method editor (IME)"):

- vajutus kollasele nupule käivitab/lõpetab/katkestab kõnetuvastuse
- svaip vasakule kustutab kursorist vasakul asuva sõna
- svaip paremale lisab reavahetuse
- topeltvajutus lisab tühiku
- vajutus klaviatuuriikoonile vahetab klaviatuuri
- pikk vajutus klaviatuuriikoonile avab klaviatuurivahetusmenüü
- vajutus otsinguikoonile käivitab otsingu (ainult otsingureal)

Kõneklaviatuuri kasutamiseks tuleb see ennem sisselülitada süsteemses menüüs
``Seaded -> Keeled ja sisestamine -> Klaviatuur ja sisestusmeetodid``.
Kõneklaviatuur kasutab vaikimisi uut kõnetuvastusteenust, kuid põhimõttelistelt
saab seda kasutada kõikide seadmesse installeeritud kõnetuvastusteenustega
(nt Kõnele grammatika-teadlik teenus ja Google'i teenus), muutes kõneklaviatuuri vastavat
seadet Kõnele seadetes.

