Versiooniajalugu
================

v0.8.xx
-------

Lisati kõneklaviatuur (nn "input method editor (IME)"):

- vajutus kollasele nupule käivitab/lõpetab kõnetuvastuse
- svaip vasakule kustutab kursorist vasakul asuva sõna
- svaip paremale lisab reavahetuse
- pikk vajutus nupu all olevale tekstile lisab tühiku
- vajutus klaviatuuriikoonile vahetab klaviatuuri (pikk vajutus avab klaviatuurivahetusmenüü)
- vajutus otsinguikoonile käivitab otsingu (ainult otsingureal)

Sisselülitamine menüüs "Seaded -> Keeled ja sisestamine -> Klaviatuur ja sisestusmeetodid".
Kõneklaviatuuri saab kasutada kõikide seadmesse installeeritud kõnetuvastusteenustega
(nt Kõnele enda teenused ja Google'i teenus), kuid testitud on seda vaid Kõnele enda teenustega.

Samuti lisati uus kõnetuvastusteenus, mis kasutab kiire tagasisidega tuvastusserverit
(https://github.com/alumae/kaldi-gstreamer-server), tänu millele saadetakse transkriptsioon
rakendusele juba rääkimise ajal ning tuvastustäpsus on oluliselt parem. (Hetkel puudub
serveril grammatikatugi, mistõttu toetab Kõnele ka varasemat grammatikatoega serverit.)
Juhul kui see teenus on määratud kogu seadmes vaikimisi kõnetuvastusteenuseks
"Seaded -> Keeled ja sisestamine -> Kõne -> Häälsisend", siis on võimalik seda kasutada
ka teistes rakendustes, nt Google'i tõlkerakendus (Google Translate) kasutab
vaikimisi määratud kõnetuvastusteenust keelte jaoks, mida ta ise ei toeta (nt eesti keel).
