<p align="center">
<img src="logo/app_name.png"/>
</p>

<p align="center">
<img src="doc/images/map-list.jpg" width="200"> &nbsp&nbsp&nbsp <img src="doc/images/trekme-example.jpg" width="200">
</p>

## <a name="TOC-Summary"></a>Sommaire

1. [Introduction](#TOC-Overview)
2. [Résumé des fonctionnalités](#TOC-Features-sum)
3. [Créer une carte](#TOC-Create-a-map)
  * [Sélectionner une zone](#TOC-Select-area)
  * [Depuis une archive](#TOC-Import-from-archive)
  * [Recevoir une carte](#TOC-Share-maps)
  * [Création manuelle](#TOC-The-hard-way)
4. [Fonctionnalités](#TOC-Features)
  * [Mesurer une distance](#TOC-Measure-distance)
  * [Afficher la vitesse](#TOC-Show-speed)
  * [Ajouter un marqueur](#TOC-Add-markers)
  * [Ajouter un repère](#TOC-Add-landmarks)
  * [Verrouiller la vue sur la position courante](#TOC-Lock-view)
  * [Visualiser un enregistrement en temps réel](#TOC-Display-live-route)
  * [Importer une trace GPX](#TOC-GPX-track-import)
  * [S'enregistrer au format GPX](#TOC-GPX-recording)
5. [Paramètres](#TOC-Settings)
  * [Démarrer sur la dernière carte](#TOC-Start-on-last-map)
  * [Dossier de téléchargement](#TOC-Download-dir)
  * [Mode de rotation](#TOC-Rotation-mode)
6. [Sauvegardez vos cartes](#TOC-Save-maps)
7. [Partage de carte](#TOC-Share-maps)
8. [Cartes IGN](#TOC-IGN-maps)
9. [Que faire si...](doc/troubleshoot/troubleshoot.fr.md)


## <a name="TOC-Overview"></a>Introduction

TrekMe est une application Android permettant de se localiser sur une carte topographique, sans 
nécessiter de connexion internet (sauf chez soi au moment de créer la carte). Il est possible de 
 télécharger des cartes IGN, USGS, SwissTopo, et OpenStreetMap (d'autres sources seront ajoutées). 
 
L'accent a été mis sur la faible consommation des ressources, pour *maximiser l'autonomie* lors d'une 
randonnée. L'application est aussi très fluide de manière générale.

## <a name="TOC-Features-sum"></a>Résumé des fonctionnalités

* Création de cartes depuis l'application:
    - France IGN (requiert une [souscription annuelle](#TOC-IGN-maps) à l'IGN)
    - Swiss Topo
	- USA : USGS
 	- Espagne IGN 
 	- OpenStreetMap
* Marqueurs (possibilité d'ajout de commentaire)
* GPX : import de trace et enregistrement
* Indicateur d'orientation
* Indicateur de vitesse
* Indicateur de distance à vol d'oiseau
* Verrouiller la vue à la position courante

## <a name="TOC-Create-a-map"></a>Créer une carte

Il y a quatre manières de créer une carte :
1. Sélectionner une zone avec une source de carte comme l'IGN par ex
2. Import d'une archive
3. Recevoir une carte d'un autre utilisateur de TrekMe (à proximité, en Wifi)
4. La faire soi-même (pour les utilisateurs avancés)

La méthode la plus facile et recommandée est la première. Ci-dessous sont décrites chacune de ces
 méthodes.

### <a name="TOC-Select-area"></a>Sélectionner une zone

Ici, on utilise une source de carte spécifique. Google map est un exemple de source très connu. Mais
 leurs cartes ne sont pas idéales pour la randonnée (on souhaite avoir des cartes plus adaptées).

Les cartes IGN sont parfaites pour cela. Elles couvrent la France entière ainsi que les DOM-TOM 
(Guadeloupe, Martinique, Réunion, Tahiti, etc.). Pour les Etats-Unis, il y a l'USGS.
Il est important de noter que tous les pays n'ont pas un service national équivalent de l'IGN. Donc 
parfois, il faut se contenter des cartes OpenStreetMap.

Certains fournisseurs de carte nécessitent une souscription, pour l'instant seulement l'IGN.

Depuis le menu principal, choisissez "Créer une carte". Un menu vous donne alors le choix entre les 
sources suivantes : 

<p align="center">
<img src="doc/images/wmts-providers-fr.jpg" width="300">
</p>

Quand vous avez fait votre choix, la carte s'affiche après un délai de quelques secondes.
Vous pouvez alors zoomer et vous déplacer sur la zone qui vous intérresse, comme dans la vidéo ci-dessous.

https://user-images.githubusercontent.com/15638794/136759759-33378909-cba7-4434-a09a-2c40cc16b71b.mp4

Il est aussi possible de rechercher un lieu particulier avec le bouton recherche. Pour l'IGN, il a des fonctionnalités 
supplémentaires comme le [choix de différentes couches](doc/ign_layers.fr.md). Quand vous avec trouvé la bonne zone, 
un bouton en bas à droite de l'écran fait apparaître une zone bleue qui définit ce qui sera téléchargé. 
Cette zone est modifiable en déplaçant les deux ronds bleus (voir la vidéo ci-dessous).

https://user-images.githubusercontent.com/15638794/136759807-dc5b183e-e181-4bae-af52-5fa2418f06cf.mp4

Quand vous êtes satisfait de votre sélection, utilisez le bouton de téléchargement. 

NB : La plupart des fournisseurs de cartes n'ont qu'une couverture partielle du globe. A l'exception 
d'OpenStreetMap, qui couvre le monde entier, l'USGS par ex ne couvre que les Etats-Unis, l'IGN Espagne 
que l'Espagne, etc.

A l'exception de l'IGN France, qui nécessite un abonnement annuel, le menu suivant s'affiche :

<p align="center">
<img src="doc/images/map-configuration.jpg" width="300">
</p>

Les fournisseurs de carte proposent différents niveaux de zoom, allant de 1 (niveau globe) à 18 
(carte très détaillée).
Dans la plupart des cas, vous ne voulez pas des niveaux 1 à 10, et le niveau 18 n'est pas nécessaire. 
C'est la raison pour laquelle le réglage par défaut est de 12 pour le zoom minimum, et 16 pour le maximum.
Ces réglages par défaut conviennent pour la plupart des usages, et il est conseillé de ne pas les
changer sauf si vous savez ce que vous faites.

La quantité d'images qui devront être téléchargées dépend directement du choix des niveaux de zoom 
min et max. Plus le niveau de zoom min est petit et plus le niveau max est grand, plus la quantité à
 télécharger est importante.
Ceci est indiqué par les nombre de transactions. Mais aussi plus simplement l'estimation de la taille
 de la carte en Mo est indiquée.
Il est important de noter que télécharger plusieurs centaines de Mo peut prendre des heures... Il est
 donc fortement recommandé de ne sélectionner que la zone dont vous avez besoin.

Quand tout est ok, utilisez le bouton "Telecharger". Un service est alors lancé, et une notification 
vous en informe. Depuis le gestionnaire de notifications de votre téléphone, vous pouvez :

* Voir la progressoin du téléchargement
* Annuler le téléchargement

Quand le service a fini le téléchargement, vous recevez une notification et une nouvelle carte est 
disponible dans la liste des cartes. Cette carte est déjà calibrée et prête à l'emploi.
Vous pouvez la personnaliser en lui associant une image de présentation qui apparaîtra à côté de son
 nom dans la liste des cartes. Pour ce faire, utilisez le bouton d'édition en dessous du nom de la 
 carte, sur la gauche.
Vous accédez alors à la configuration de la carte, où vous pouvez :

* Changer l'image de présentation
* Changer la projection de la carte (seulement si vous savez ce que vous faites)
* Changer les points de calibration (seulement si vous savez ce que vous faites)
* Changer le nom de la carte
* Sauvegarder la carte

Attention, il ne faut pas changer la projection ni les points de calibration d'une carte que vous 
avez téléchargé. Il s'agit d'options pour les utilisateurs avertis. Une erreur à ce niveau 
introduirait un biais dans votre positionnement sur la carte.

### <a name="TOC-Import-from-archive"></a>Depuis une archive

Une carte peut aussi être créée en important une archive d'une carte existante. L'archive peut avoir 
été faite par vous même ou quelqu'un d'autre (voir plus bas pour créer une archive). Elle se présente
 sous la forme d'un fichier zip.
Pour importer une archive, utilisez le menu principal puis "Importer une carte". Appuyez alors sur le
bouton "Importer depuis un dossier", au milieu de l'écran. Naviguez alors vers le dossier qui contient
la ou les archives et sélectionnez-le. TrekMe liste alors les archives reconnues, que vous pouvez 
importer individuellement.

### <a name="TOC-Import-from-sharing"></a>Recevoir une carte

Reportez-vous à la section [Partage de carte](#TOC-Share-maps).

### <a name="TOC-The-hard-way"></a>Création manuelle - le plus difficile

Il est possible d'utiliser votre propre carte si vous la scannez et suivez le tutoriel pour l'utiliser
avec TrekMe.
C'est réservé aux experts. Cette méthode n'est en aucun cas nécessaire, mais grâce à cette fonctionnalité, 
on peut mettre n'importe quelle carte dans TrekMe.
Il faut avoir des connaissances en géolocalisation, et il est recommandé d'être familiarisé avec les
 termes suivants :

[Map projection](https://en.wikipedia.org/wiki/Map_projection),
[WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84),
[Mercator](https://en.wikipedia.org/wiki/Mercator_projection?oldid=9506890).

Pour les personnes voulant apprendre, il est conseillé de lire ce [guide](UserGuide.md) (en anglais).

Ensuite, poursuivez avec le [Guide de création manuelle de carte](MapCreation-Manual.md).

   
## <a name="TOC-Features"></a>Fonctionnalités

### <a name="TOC-Measure-distance"></a>Mesurer une distance

Il y deux manières de mesurer une distance dans TrekMe :

*Distance à vol d'oiseau*

C'est une option en haut à droite alors qu'une carte est affichée: "Mesurer une distance".
Ajuster la mesure en déplaçant les deux ronds bleus.

<p align="center">
<img src="doc/images/distance.jpg" width="300">
</p>

*Distance le long d'une trace*

Quand on fait une randonnée en suivant une trace, il est parfois utile de connaître la distance entre
deux points le long de la trace. Cela permet par exemple de connaître la distance restante avec un 
point particulier, et évaluer si on a assez de temps pour faire l'aller-retour.

Il s'agit d'un mode à activer/désactiver dans les options d'une carte: "Distance sur trace".
Lorsque activée, deux rond bleus appraissent sur la trace la plus proche du centre de l'écran. La
portion de la trace entre les deux ronds est en rouge et sa longueur est indiquée:

<p align="center">
<img src="doc/images/dist-on-track.jpg" width="300">
</p>

La distance affichée tient compte du dénivellé *si* la trace contient l'information d'altitude pour
chacun des points.

### <a name="TOC-Show-speed"></a>Afficher la vitesse

C'est une option en haut à droite alors qu'une carte est affichée.
La vitesse s'affiche en km/h au bout de quelques secondes.

<p align="center">
<img src="doc/images/speed.jpg" width="300">
</p>

Selon la taille de votre écran, un bouton peut rendre cette fonctionnalité directement accessible.

### <a name="TOC-Add-markers"></a>Ajout de marqueurs

Utilisez le bouton d'ajout de marqueur, ce qui affiche un nouveau marqueur au centre de l'écran, 
comme celui-ci :

<p align="center">
<img src="doc/images/new-marker.jpg" width="300">
</p>

Avec ses flèches rouge qui tournent autour, il indique qu'il peut être déplacé. Pour cela appuyez 
avec un doigt dans la zone bleue et déplacez le marqueur à l'endroit désiré.
Quand vous êtes satisfait de sa position, "appuyez" une fois sur le marqueur rouge. Il change alors 
de forme et la zone bleue disparaît. Cela indique que le marqueur est désormais fixé à son emplacement.

Si on appuie sur le marqueur, une bulle comme celle-ci s'affiche :

<p align="center">
<img src="doc/images/marker-popup2.jpg" width="300">
</p>

On peut alors :

* Changer le nom ou le commentaire du marqueur 
* Supprimer le marqueur
* Déplacer le marqueur (il reprend sa forme avec les flèches qui tournent, indiquant qu'il peut être 
déplacé)

Voici un exemple de fiche d'un marqueur :

<p align="center">
<img src="doc/images/marker-edit.jpg" width="300">
</p>

La plupart du temps, on se contente de modifier seulement le nom ou le commentaire.
Rien n'est modifié tant que vous ne sauvegardez pas vos modifications (bouton "disquette" en haut).

### <a name="TOC-Add-landmarks"></a>Ajout de points de repère

Un point de repère est un marqueur spécial. Une ligne de couleur violette se dessine entre votre 
position actuelle et le repère. De cette manière vous pouvez vous fixer un objectif éloigné et 
toujours voir à l'écran la direction vers ce repère.

Pour ajouter un point de repère, c'est le même principe que pour les marqueurs mais on utilise 
l'icone de phare (comme un amer en navigation maritime).

<p align="center">
<img src="doc/images/landmark-1.jpg" width="300">
</p>

En général, on affiche en même temps l'orientation. On peut aussi en ajouter plusieurs :

<p align="center">
<img src="doc/images/landmark-2.jpg" width="300">
</p>

### <a name="TOC-Lock-view"></a>Verrouiller la vue sur la position courante

Parfois, on veut que la vue reste centrée sur notre position (on ne veut pas que le point bleu sorte 
de l'écran). Pour cela, il faut activer le verrouillage sur la position. Utilisez alors le menu 
comme ci-dessous :

<p align="center">
<img src="doc/images/menu-map-view-highlight.jpg" width="300">
</p>

Puis sélectionnez "Verrouiller sur la position". Dans ce mode, à chaque fois que l'application reçoit
 votre position (environ toutes les secondes), la vue se déplace et le point bleu reste au centre.

### <a name="TOC-Display-live-route"></a>Visualiser un enregistrement en temps réel

Par défaut, lorsque vous lancez un enregistrement GPX (depuis le menu "Enregistrement du parcours"), 
l'enregistrement en cours s'affiche sur votre carte, sous forme de trace jaune.
Cela ne fonctionne que pour les cartes qui couvrent la zone dans laquelle vous êtes au moment de 
l'enregistrement.

Même si vous quittez TrekMe, vous retrouverez la trace en temps réel sur votre carte, tant que l'enregistrement est en cours.

<p align="center">
<img src="doc/images/live-route.jpg" width="300">
</p>

### <a name="TOC-GPX-track-import"></a>Import d'un fichier GPX

Alors que vous visionnez une carte, utilisez le menu en haut à droite :

<p align="center">
<img src="doc/images/upper-right-menu-fr.jpg" width="300">
</p>

Puis, sélectionnez "Gérer les traces":

<p align="center">
<img src="doc/images/manage-tracks-fr.jpg" width="300">
</p>

La liste des traces disponibles pour votre carte s'affiche alors (il est possible à ce stade qu'il y 
en ait aucune) :

<p align="center">
<img src="doc/images/track-list-fr.jpg" width="300">
</p>

Vous pouvez alors :

* Importer un fichier gpx avec le bouton d'import en bas à droite
* Assigner un couleur pour chaque trace
* Gérer la visibilité des traces déjà importées
* Supprimer des traces en les faisant glisser à droite ou à gauche (cela n'affecte en rien le fichier gpx)

Avec le menu en haut à droite, qui appraît quand vous sélectionnez une trace, vous pouvez :

* Renommer la trace sélectionnée
* Aller directement sur la trace sélectionnée sur la carte (cette fonctionnalité n'est disponible qu'avec l'offre TrekMe Extended).

### <a name="TOC-GPX-recording"></a>Enregistrement GPX

Il est possible d'enregistrer votre parcours au format GPX, pour ensuite l'importer dans une carte 
ou le partager.

Depuis le menu principal allez à "Enregistrement GPX". Vous arrivez à une interface comme
 celle-ci :

<p align="center">
<img src="doc/images/gpx-recordings-fr.jpg" width="300">
</p>

Un enregistrement peut être démarré, arrêté ou mis en pause depuis le panneau "Commandes".
Quand un enregistrement est en cours, un service spécifique est démarré, qui fonctionne même si TrekMe 
est arrêté. Ce service s'arrête dès que vous l'arrêtez depuis le panneau "Commandes".
Si vous avez Android 10 ou plus, vous devez vous assurer que TrekMe a l'autorisation de localisation 
en mode "Toujours autoriser", et pas seulement si l'application est en cours d'utilisation. Sinon,
l'enregistrement n'enregistrera parfois pas de points et des lignes droites apparaîtront.

Un indicateur dans le panneau "Service de localisation" affiche le statut du service.

Un dernier panneau affiche la liste des enregistrements effectués.
En sélectionnant un enregistrement, quatre boutons en bas à gauche vous permettent respectivement de :

* Renommer le fichier gpx
* Importer la trace dans une carte existante (un menu vous donne alors le choix de la carte)
* Partager un ou plusieurs fichiers gpx (par email, par exemple)
* Afficher le profil altimétrique

**Mode de sélection multiple**

Appuyez deux secondes sur un enregistrement. 
Pour revenir au mode de sélection normal, appuyez à nouveau deux secondes sur un enregistrement.

**Partage d'un ou plusieurs enregistrements**

Que vous soyez en mode de sélection simple ou multiple, le bouton de partage est disponible :
<p align="center">
<img src="doc/images/share-recording.jpg" width="300">
</p>

**Supprimer un enregistrement**

Un bouton rouge en bas à droite avec une icone de corbeille est accessible en mode de sélection
simple ou multiple. Attention, si vous pressez ce bouton rouge, tous les enregistrements sélectionnés
(cad sur fond bleu) seront **définitivement** supprimés.
  
**Import automatique d'une trace**

Lorsque vous arrêtez un enregistrement, le fichier GPX correspondant s'ajoute dans la liste des
enregistrements _et_ la trace est automatiquement importée dans toutes les cartes qui peuvent
l'afficher. Il est donc très facile de vous enregistrer et d'afficher vos traces sur vos cartes
favorites.

**Profil altimétrique**

Le profil altimétrique vous permet de connaître l'altitude en tout point du parcours, et en particulier
les altitudes min, max, et le dénivelé (non-cumulé).

<p align="center">
<img src="doc/images/ele-profile-fr.jpg" width="300">
</p>

## <a name="TOC-Settings"></a>Paramètres

Les paramètres de TrekMe sont accessibles depuis le menu principal > Paramètres.

### <a name="TOC-Start-on-last-map"></a>Démarrer sur la dernière carte

Par défaut, TrekMe démarre sur la liste des cartes. Mais vous pouvez aussi démarrer sur la dernière 
carte visitée. Dans la section "Général" > "Lancer TrekMe sur" :

<p align="center">
<img src="doc/images/start_on_fr.jpg" width="300">
</p>


### <a name="TOC-Download-dir"></a>Dossier de téléchargement

Par défaut, TrekMe enregistre tout sur la mémoire interne. Mais si vous avez une carte SD, **et** que
 celle-ci est montée en tant que **stockage amovible**, alors vous pouvez l'utiliser pour stocker 
 certaines de vos cartes.

**Attention**

Quelque soit la version d'Android, toutes les cartes stockées sur carte SD seront supprimées si TrekMe est 
désinstallé (c'est une contrainte du système Android). 
Reportez-vous à la section [Sauvegardez vos cartes](#TOC-Save-maps) pour voir comment contourner cette
contrainte.

Dans la section Téléchargment > "Répertoire de téléchargement", vous avez le choix entre deux répertoires
 si vous avez une carte SD. Sinon, vous n'avez d'autre choix que d'utiliser la mémoire interne.

<p align="center">
<img src="doc/images/download_dir.jpg" width="300">
</p>

Le premier répertoire correspond toujours à la mémoire interne. Le second, s'il y en a un, à la carte 
SD. Ce dossier est `Android/data/com.peterlaurence.trekme/downloaded`.

Une fois le répertoire modifié, votre prochain téléchargement de carte utilisera ce dossier. Mais 
les cartes existantes ne sont pas déplacées.

### <a name="TOC-Rotation-mode"></a>Mode de rotation

Vous pouvez sélectionner parmi trois modes de rotation:

* Aucune rotation (mode par défaut)
* Tourner la carte selon l'orientation
* Rotation libre

*Tourner la carte selon l'orientation*

Dans ce mode, la carte s'oriente en fonction de l'orientation de votre appareil *si* vous activez l'affichage de
l'orientation (lors du visionnage d'une carte, menu en haut à droite > Afficher l'orientation).
Lorsque l'orientation est activée, apparaît également en bas à droite une petite boussole, dont
l'aiguille rouge indique le Nord. Vous pouvez à tout moment choisir de désactiver temporairement la 
rotation de la carte en désactivant l'affichage de l'orientation. Dans ce cas, la carte s'aligne sur
le Nord et la boussole disparaît. A noter que dans ce mode, appuyer sur la boussole n'a aucun effet.
Par exemple:

<p align="center">
<img src="doc/images/rotation-oriented.jpg" width="300">
</p>

*Rotation libre*

Dans ce mode, vous pouvez tourner la carte à volonté. La boussole s'affiche toujours, et appuyer dessus
aligne la carte sur le Nord. Vous pouvez aussi afficher ou masquer l'orientation, sans conséquence sur
l'alignement de la carte.

### <a name="TOC-Save-maps"></a>Sauvegardez vos cartes

A partir d'Android 10, toutes les cartes (qu'elles soient sur mémoire interne ou carte SD) sont supprimées
si TrekMe est désinstallé. Il vous est donc fortement conseillé d'utiliser la fonctionnalité de sauvegarde
intégrée à TrekMe. Cela vous permettra de restaurer vos cartes si vous changez de téléphone par exemple.

Pour créer une archive, depuis la liste des cartes, utilisez le bouton "MODIFIER", en bas à gauche :

<p align="center">
<img src="doc/images/bali-fr.jpg" width="300">
</p>

Vous arrivez alors dans les options pour cette carte. Tout en bas, il y a un bouton "Sauvegarder".
Appuyez dessus ; un message vous explique alors que vous devez choisir le dossier dans votre téléphone
où l'archive sera créée. Vous pouvez en effet choisir le dossier que vous voulez (ne choisissez pas
un dossier interne à l'application TrekMe..). Vous pouvez aussi créer un nouveau dossier et le nommer
(par exemple) "TrekMe_sauvegardes".
Si vous continuez la procédure, l'archive va être créée et vous pouvez suivre la progression dans la
zone de notification du téléphone.

Une archive contient tout ce qui est relatif à la carte (calibration, traces, points d'intérêt, etc).

Une fois archivée, une carte peut être restaurée avec la fonctionnalité d'import de carte.

### <a name="TOC-Share-maps"></a>Partage de carte

Une carte est parfois volumineuse et longue à télécharger. Quand un(e) ami(e) a aussi TrekMe, vous
pouvez lui envoyer directement une de vos cartes. Cela fonctionne avec la Wifi, sans nécessité d'être
à proximité d'une box ou un hotspot : la communication se fait directement avec l'autre appareil (qui
lui aussi doit activer sa Wifi). Veillez donc bien à activer la Wifi pour la suite, et faites en sorte 
que les deux appareils soient relativement proches (même si des transferts ont réussi avec plusieurs
mètres de distance, l'expérience montre que plus ils sont proches, moins il y a de risque d'erreur).
Point important : la localisation soit être active sur les deux appareils pendant la procédure.

Rendez-vous dans le menu principal > "Recevoir et envoyer". L'interface propose un bouton pour
recevoir une carte, et un autre pour envoyer. Lorsque vous choisissez de recevoir, l'interface indique
qu'elle est en attente de réception. Les deux appareils cherchent à établir un canal de communication.
Cette phase peut durer plusieurs minutes ; il faut être patient(e).
Pendant ce temps, votre ami(e) utilise le bouton d'envoi, et choisit alors la carte à envoyer.

Quand la connexion est établie, le transfert commence et vous pouvez voir la progression en temps réel.
Si tout se passe bien, le transfert va jusqu'au bout et une magnifique emoticon vous l'indique. Dans
le cas contraire, et en particulier si le transfert s'est interrompu en cours de chargement, une nouvelle
tentative peut réussir (ce sont les aléas de la Wifi).

Si les deux appareils mettent vraiment trop de temps à se connecter et à débuter le transfert (plus
de 5 min), essayez d'arrêter et relancer respectivement la réception et l'envoi. En dernier recours,
redémarrer les deux apprareils puis re-tentez la procédure.

## <a name="TOC-IGN-maps"></a>Cartes IGN

TrekMe vous propose un abonnement annuel, avec une semaine d'essai gratuite sans engagement. 
Pourquoi n'est-ce pas gratuit? L'esprit de TrekMe est la consultation hors ligne des cartes. 
Bien que certaines ressources sont désormais libres, les cartes IGN les plus demandées comme 
"Plan IGN" sont toujours basées sur des 
[ressources payantes](https://www.ign.fr/sites/default/files/2020-12/cp_gratuite_donnees_2020.pdf). 
Pour cette raison, et aussi pour rémunérer le travail considérable que représente le développement
de cette application, les cartes IGN sont proposées au travers une offre "TrekMe Extended", disponible
sous forme d'abonnement à l'année ou au mois.
