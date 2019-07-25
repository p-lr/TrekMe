<p align="center">
<img src="logo/app_name.png"/>
</p>

1. [Introduction](#TOC-Overview)
2. [Résumé des fonctionnalités](#TOC-Features-sum)
3. [Créer une carte](#TOC-Create-a-map)
  * [Sélectionner une zone](#TOC-Select-area)
  * [Depuis une archive](#TOC-Import-from-archive)
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
6. [Que faire si...](doc/troubleshoot/troubleshoot.fr.md)


## <a name="TOC-Overview"></a>Introduction

TrekMe est une application Android permettant de se localiser sur une carte topographique, sans nécessiter de connexion internet (sauf chez soi au moment de créer la carte). La différence principale avec les autres applications de la même catégorie est la source des cartes. Il est possible de télécharger des cartes IGN, USGS, et OpenStreetMap (d'autres sources seront ajoutées). Mais il est aussi possible d'utiliser votre propre carte si vous la scannez et suivez le tutoriel pour l'utiliser avec TrekMe.
L'accent a été mis sur la faible consommation des ressources, pour maximiser l'autonomie lors d'une randonnée.

Cartes IGN : contrairement à certaines applications qui font payer à la carte, TrekMe ne vous coûtera que 6€ par an. Vous serez guidé pour vous inscrire sur le site de l'IGN. Vous obtiendrez vos propres identifiants, pour accéder à un volume annuel de cartes qui est largement suffisant pour un usage normal.
Il faut pour cela suivre scrupuleusement le [tutoriel](https://github.com/peterLaurence/TrekMe/wiki/Tutoriel-:-obtenir-une-cl%C3%A9-IGN).
Cependant, l'IGN impose que toute personne stockant ses cartes pour un usage hors connexion doit s'acquitter d'une ["license d'exploitation grand public numérique"](http://professionnels.ign.fr/doc/bareme_LCO_2019.pdf).
Cette license coûte 6€ par an et TrekMe vous invitera à la régler quand vous voudrez créer votre première carte IGN. Si au bout d'un an vous ne renouvelez pas votre license, vous ne pourrez plus utiliser votre clé d'API pour télécharger de nouvelles cartes.

## <a name="TOC-Features-sum"></a>Résumé des fonctionnalités

* Création de cartes depuis l'application:
	- USA : USGS
	- France IGN (requiert une souscription **annuelle de 6€** à l'IGN)
 	- Espagne IGN 
 	- OpenStreetMap
* Marqueurs (possibilité d'ajout de commentaire)
* GPX : import de trace et enregistrement
* Indicateur d'orientation
* Indicateur de vitesse
* Indicateur de distance à vol d'oiseau
* Vérouiller la vue à la position courante

## <a name="TOC-Create-a-map"></a>Créer une carte

Il y a trois manières de créer une carte :
1. Sélectionner une zone avec une source de carte comme l'IGN par ex
2. Import d'une archive
3. La faire soi-même (pour les utilisateurs avancés)

La méthode la plus facile et recommandée est la première. Ci-dessous sont décrites ces trois méthodes.

### <a name="TOC-Select-area"></a>Sélectionner une zone

Ici, on utilise une source de carte spécifique. Google map est un exemple de source très connu. Mais leurs cartes ne sont pas idéales pour la randonnée (on souhaite avoir des cartes plus adaptées).

Les cartes IGN sont parfaites pour cela. Elles couvrent la France entière ainsi que les DOM-TOM (Guadeloupe, Martinique, Réunion, Tahiti, etc.). Pour les Etats-Unis, il y a l'USGS.
Il est important de noter que tous les pays n'ont pas un service national équivalent de l'IGN. Donc parfois, il faut se contenter des cartes OpenStreetMap.

Certains fournisseurs de carte nécessitent une souscription, pour l'instant seulement l'IGN.

Depuis le menu principal, choisissez "Créer une carte". Un menu vous donne alors le choix entre les sources suivantes : 

<p align="center">
<img src="doc/tuto/wmts-providers.jpg" width="300">
</p>

A l'exception de l'IGN France, pour laquelle une souscription annuelle est nécessaire, vous pouvez directement sélectionner une source et continuer.

<p align="center">
<img src="doc/tuto/select-area.jpg" width="300">
</p>

Vous pouvez alors zoomer et vous déplacer sur la zone qui vous intérresse. Un bouton en haut à droite, ressemblant à un carré, vous permet de faire apparaître une zone modifiable (en bleu).
Quand vous êtes satisfait de votre sélection, utilisez le bouton de téléchargement en bas à droite.

NB : la plupart des fournisseurs de cartes n'ont qu'une couverture partielle du globe. A l'exception d'OpenStreetMap, qui couvre le monde entier, l'USGS par ex ne couvre que les Etats-Unis, l'IGN Espagne que l'Espagne, etc.

Un menu tel que celui-ci s'affiche :

<p align="center">
<img src="doc/tuto/map-configuration.jpg" width="300">
</p>

Les fournisseurs de carte proposent différents niveaux de zoom, allant de 1 (niveau globe)  à 18 (carte très détaillée).
Dans la plupart des cas, vous ne voulez pas des niveaux 1 à 10, et le niveau 18 n'est pas nécessaire. C'est la raison pour laquelle le réglage par défaut est de 12 pour le zoom minimum, et 16 pour le maximum.

La quantité d'images qui devront être téléchargées dépend directement du choix des niveaux de zoom min et max. Plus le niveau de zoom min est petit et plus le niveau max est grand, plus la quantité à télécharger est importante.
Ceci est indiqué par les nombre de transactions (ceux qui auront suivi le tutoriel d'inscription à l'IGN sauront de quoi il s'agit). Mais aussi plus simplement l'estimation de la taille de la carte en Mo est indiquée.
Il est important de noter que télécharger plusieurs centaines de Mo peut prendre des heures... Il est donc fortement recommandé de ne sélectionner que la zone dont vous avez besoin.

Quand tout est ok, utilisez le bouton "Telecharger". Un service est alors lancé, et une notification vous en informe. Depuis le gestionnaire de notifications de votre téléphone, vous pouvez :

* Voir la progressoin du téléchargement
* Annuler le téléchargement

Quand le service a fini le téléchargement, vous recevez une notification et une nouvelle carte est disponible dans la liste des cartes. Cette carte est déjà calibrée et prête à l'emploi.
Vous pouvez la personnaliser en lui associant une image de présentation qui apparaîtra à côté de son nom dans la liste des cartes. Pour ce faire, utilisez le bouton d'édition en dessous du nom de la carte, sur la gauche.
Vous accédez alors à la configuration de la carte, où vous pouvez :

* Changer l'image de présentation
* Changer la projection de la carte (seulement si vous savez ce que vous faites)
* Changer les points de calibration (seulement si vous savez ce que vous faites)
* Changer le nom de la carte
* Supprimer la carte


### <a name="TOC-Import-from-archive"></a>Depuis une archive

Une carte peut aussi être créée en important une archive d'une carte existante. L'archive peut avoir été faite par vous même ou quelqu'un d'autre. 
Pour créer une archive, depuis la liste des cartes, utilisez le bouton qui ressemble à une disquette, sous l'image de présentation :

<p align="center">
<img src="doc/tuto/bali.jpg" width="300">
</p>

Cela  crée un fichier zip (que l'on appelle archive) dans le dossier `trekme/archives` de la mémoire interne de votre téléphone.
Pour utiliser une archive faite par quelqu'un d'autre que vous :
1. Copiez le fichier zip dans le dossier `trekme` ou n'importe quel de ses sous dossiers
2. Menu > "Importer"
3. Utilisez le bouton "Importer" de l'archive de votre choix

Cette fonctionnalité d'archive peut aussi être utilisée pour de la sauvegarde, puisque tout ce qui est relatif à la carte est enregistré (calibration, traces, points d'intérêt, etc).

### <a name="TOC-The-hard-way"></a>Création manuelle - le plus difficile

C'est réservé aux experts. Cette méthode n'est en aucun cas nécessaire, mais grâce à cette fonctionnalité, on peut mettre n'importe quelle carte dans TrekMe.
Il faut avoir des connaissances en géolocalisation, et il est recommandé d'être familiarisé avec les termes suivants :

[Map projection](https://en.wikipedia.org/wiki/Map_projection),
[WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84),
[Mercator](https://en.wikipedia.org/wiki/Mercator_projection?oldid=9506890).

Pour les personnes voulant apprendre, il est conseillé de lire ce [guide](UserGuide.md) (en anglais).

Ensuite, poursuivez avec le [Guide de création manuelle de carte](MapCreation-Manual.md).

   
## <a name="TOC-Features"></a>Fonctionnalités

### <a name="TOC-Measure-distance"></a>Mesurer une distance

C'est une option en haut à droite alors qu'une carte est affichée.
Ajuster la mesure en déplaçant les deux ronds bleus. C'est une distance "à vol d'oiseau".

<p align="center">
<img src="doc/tuto/distance.jpg" width="300">
</p>

### <a name="TOC-Show-speed"></a>Afficher la vitesse

C'est une option en haut à droite alors qu'une carte est affichée.
La vitesse s'affiche en km/h au bout de quelques secondes.

<p align="center">
<img src="doc/tuto/menu-map-view-highlight.jpg" width="300">
</p>

Selon la taille de votre écran, un bouton peut rendre cette fonctionnalité directement accessible.

### <a name="TOC-Add-markers"></a>Ajout de marqueurs

Utilisez le bouton d'ajout de marqueur, ce qui affiche un nouveau marqueur au centre de l'écran, comme celui-ci :

<p align="center">
<img src="doc/tuto/new-marker.jpg" width="300">
</p>

Avec ses flèches rouge qui tournent autour, il indique qu'il peut être déplacé. Pour cela appuyez avec un doigt dans la zone bleue et déplacez le marqueur à l'endroit désiré.
Quand vous êtes satisfait de sa position, "appuyez" une fois sur le marqueur rouge. Il change alors de forme et la zone bleue disparaît. Cela indique que le marqueur est désormais fixé à son emplacement.

Si on appuie sur le marqueur, une bulle comme celle-ci s'affiche :

<p align="center">
<img src="doc/tuto/marker-popup2.jpg" width="300">
</p>

On peut alors :

* Changer le nom ou le commentaire du marqueur 
* Supprimer le marqueur
* Déplacer le marqueur (il reprend sa forme avec les flèches qui tournent, indiquant qu'il peut être déplacé)

Voici un exemple de fiche d'un marqueur :

<p align="center">
<img src="doc/tuto/marker-edit.jpg" width="300">
</p>

La plupart du temps, on se contente de modifier seulement le nom ou le commentaire.
Rien n'est modifié tant que vous ne sauvegardez pas vos modifications (bouton "disquette" en haut).

### <a name="TOC-Add-landmarks"></a>Ajout de points de repère

Un point de repère est un marqueur spécial. Une ligne de couleur violette se dessine entre votre position actuelle et le repère. De cette manière vous pouvez vous fixer un objectif éloigné et toujours voir à l'écran la direction vers ce repère.

Pour ajouter un point de repère, c'est le même principe que pour les marqueurs mais on utilise l'icone de phare (comme un amer en navigation maritime).

<p align="center">
<img src="doc/tuto/landmark-1.jpg" width="300">
</p>

En général, on affiche en même temps l'orientation. On peut aussi en ajouter plusieurs :

<p align="center">
<img src="doc/tuto/landmark-2.jpg" width="300">
</p>

### <a name="TOC-Lock-view"></a>Verrouiller la vue sur la position courante

Parfois, on veut que la vue reste centrée sur notre position (on ne veut pas que le point bleu sorte de l'écran). Pour cela, il faut activer le verrouillage sur la position. Utilisez alors le menu comme ci-dessous :

<p align="center">
<img src="doc/tuto/menu-map-view-highlight.jpg" width="300">
</p>

Puis sélectionnez "Verrouiller sur la position". Dans ce mode, à chaque fois que l'application reçoit votre position (environ toutes les secondes), la vue se déplace et le point bleu reste au centre.

### <a name="TOC-Display-live-route"></a>Visualiser un enregistrement en temps réel

Par défaut, lorsque vous lancez un enregistrement GPX (depuis le menu "Enregistrement du parcours"), l'enregistrement en cours s'affiche sur votre carte, sous forme de trace jaune.
Cela ne fonctionne que pour les cartes qui couvrent la zone dans laquelle vous êtes au moment de l'enregistrement.

Même si vous quittez TrekMe, vous retrouverez la trace en temps réel sur votre carte, tant que l'enregistrement est en cours.

<p align="center">
<img src="doc/tuto/live-route.jpg" width="300">
</p>

### <a name="TOC-GPX-track-import"></a>Import d'un fichier GPX

Alors que vous visionnez une carte, utilisez le bouton suivant :

<p align="center">
<img src="doc/tuto/manage-tracks.jpg" width="300">
</p>

La liste des traces disponibles pour votre carte s'affiche alors (il est possible à ce stade qu'il y en ait aucune) :

<p align="center">
<img src="doc/tuto/track-list.jpg" width="300">
</p>

Vous pouvez alors :

* Importer un fichier gpx avec le bouton d'import en haut
* Gérer la visibilité des traces déjà importées
* Supprimer des traces en les faisant glisser à droite ou à gauche (cela n'affecte en rien le fichier gpx)

### <a name="TOC-GPX-recording"></a>Enregistrement GPX

Il est possible d'enregistrer votre parcours au format GPX, pour ensuite l'importer dans une carte ou le partager.

Depuis le menu principal allez à "Enregistrement du parcours". Vous arrivez à une interface comme celle-ci :

<p align="center">
<img src="doc/tuto/gpx-recording.jpg" width="300">
</p>

Un enregistrement peut être démarré ou arrêté depuis le panneau "Commandes".
Quand un enregistrement est en cours, un service spécifique est démarré, qui fonctionne même si TrekMe est arrêté. Ce service s'arrête dès que vous l'arrêtez depuis le panneau "Commandes".

Un indicateur dans le panneau "Service de localisation" affiche le statut du service.

Un dernier panneau affiche la liste des enregistrements effectués.
En sélectionnant un enregistrement, trois boutons en bas à gauche vous permettent respectivement de :

* renommer le fichier gpx
* l'importer dans une carte existante (un menu vous donne alors le choix de la carte)
* partager un ou plusieurs fichiers gpx

**Mode de sélection multiple**

Appuyez deux secondes sur un enregistrement. 
Pour revenir au mode de sélection normal, appuyez à nouveau deux secondes sur un enregistrement.

**Partage d'un ou plusieurs enregistrements**

Que vous soyez en mode de sélection simple ou multiple, le bouton de partage est disponible :
<p align="center">
<img src="doc/tuto/share-recording.jpg" width="300">
</p>

**Supprimer un enregistrement**

Passez en mode de sélection multiple. Un bouton rouge en bas à droite apparaît. Attention, si vous pressez ce bouton rouge, tous les enregistrement sélectionnés (cad sur fond bleu) seront **définitivement**  supprimés. 


## <a name="TOC-Settings"></a>Paramètres

Les paramètres de TrekMe sont accessibles depuis le menu principal > Paramètres.

### <a name="TOC-Start-on-last-map"></a>Démarrer sur la dernière carte

Par défaut, TrekMe démarre sur la liste des cartes. Mais vous pouvez aussi démarrer sur la dernière carte visitée. Dans la section "Général" > "Lancer TrekMe sur" :

<p align="center">
<img src="doc/tuto/start_on_fr.jpg" width="300">
</p>


### <a name="TOC-Download-dir"></a>Dossier de téléchargement

Par défaut, TrekMe enregistre tout sur la mémoire interne. Mais si vous avez une carte SD, **et** que celle-ci est montée en tant que **stockage amovible**, alors vous pouvez l'utiliser pour stocker certaines de vos cartes.

**Attention**

Toutes les cartes stockées sur carte SD seront supprimées si TrekMe est désinstallée (c'est une contrainte du système Android). La seule manière de conserver de manière sûre vos cartes est d'utiliser la mémoire interne.
Cependant, vous pouvez contourner cette limitation : faites une sauvegarde, puis déplacez l'archive zip créée quelque part sur votre carte SD (dans un autre dossier que le dossier Android).

Dans la section Téléchargment > "Répertoire de téléchargement", vous avez le choix entre deux répertoires si vous avez une carte SD. Sinon, vous n'avez d'autre choix que d'utiliser la mémoire interne.

<p align="center">
<img src="doc/tuto/download_dir.jpg" width="300">
</p>

Le premier répertoire correspond toujours à la mémoire interne. Le second, s'il y en a un, à la carte SD. Ce dossier est `Android/data/com.peterlaurence.trekme/downloaded`.

Une fois le répertoire modifié, votre prochain téléchargement de carte utilisera ce dossier. Mais les cartes existantes ne sont pas déplacées.