![Logo](logo/app_name.png)

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
  * [Importer une trace GPX](#TOC-GPX-track-import)
  * [S'enregistrer -> GPX](#TOC-GPX-recording)


## <a name="TOC-Overview"></a>Introduction

TrekAdvisor est une application Android permettant de se localiser sur une carte topographique, sans nécessiter de connexion internet (sauf chez soi au moment de créer la carte). La différence principale avec les autres applications de la même catégorie est la source des cartes. Il est possible de télécharger des cartes IGN, USGS, et OpenStreetMap (d'autres sources seront ajoutées). Mais il est aussi possible d'utiliser votre propre carte si vous la scannez et suivez le tutoriel pour l'utiliser avec TrekAdvisor.
L'accent a été mis sur la faible consommation des ressources, pour maximiser l'autonomie lors d'une randonnée.

Cartes IGN : contrairement à certaines applications qui font payer un abonnement, TrekAdvisor fonctionne différemment en vous guidant pour vous inscrire sur le site de l'IGN. Vous pouvez alors obtenir gratuitement vos propres identifiants, pour accéder à un volume annuel de cartes qui est largement suffisant pour un usage normal.
Il faut pour cela suivre scrupuleusement le [tutoriel](https://github.com/peterLaurence/TrekAdvisor/wiki/Tutoriel-:-obtenir-une-cl%C3%A9-IGN).

## <a name="TOC-Features-sum"></a>Résumé des fonctionnalités

* Création de cartes depuis l'application:
	- USA : USGS
	- France IGN (requiert une souscription **gratuite** à l'IGN)
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

Certains fournisseurs de carte nécessitent une souscription, gratuite pour une utilisation personnelle.

Depuis le menu principal, choisissez "Créer une carte". Un menu vous donne alors le choix entre les sources suivantes : 

<p align="center">
<img src="doc/tuto/wmts-providers.jpg" width="300">
</p>

A l'exception de l'IGN France, pour laquelle une souscription **gratuite** est nécessaire, vous pouvez directement sélectionner une source et continuer.

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

Cela  crée un fichier zip (que l'on appelle archive) dans le dossier `trekadvisor/archives` de la mémoire interne de votre téléphone.
Pour utiliser une archive faite par quelqu'un d'autre que vous :
1. Copiez le fichier zip dans le dossier `trekadvisor` ou n'importe quel de ses sous dossiers
2. Menu > "Importer"
3. Utilisez le bouton "Importer" de l'archive de votre choix

Cette fonctionnalité d'archive peut aussi être utilisée pour de la sauvegarde, puisque tout ce qui est relatif à la carte est enregistré (calibration, traces, points d'intérêt, etc).

### <a name="TOC-The-hard-way"></a>Création manuelle - le plus difficile

C'est réservé aux experts. Cette méthode n'est en aucun cas nécessaire, mais grâce à cette fonctionnalité, on peut mettre n'importe quelle carte dans TrekAdvisor.
Il faut avoir des connaissances en géolocalisation, et il est recommandé d'être familiarisé avec les termes suivants :

[Map projection](https://en.wikipedia.org/wiki/Map_projection),
[WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84),
[Mercator](https://en.wikipedia.org/wiki/Mercator_projection?oldid=9506890).

Pour les personnes voulant apprendre, il est conseillé de lire ce [guide](UserGuide.md) (en anglais).

Then, proceed with the [Manual map creation guide](MapCreation-Manual.md).

   
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

### <a name="TOC-Add-markers"></a>Add markers

Press the marker button to add a new marker at the center of the screen:

<p align="center">
<img src="doc/tuto/new-marker.jpg" width="300">
</p>

With its reds arrows turning around it, its shows that it can be moved by dragging the blue circle.
When you're satisfied with its position, tap on the red circle at the center. It then morphs to its static form.

Tapping a marker displays a popup:

<p align="center">
<img src="doc/tuto/marker-popup2.jpg" width="300">
</p>

From here you can:

* Edit the marker (change its name and set a comment, see below)
* Delete it
* Move it

Here is the marker edition view:

<p align="center">
<img src="doc/tuto/marker-edit.jpg" width="300">
</p>

Nothing is changed until you save your changes.

### <a name="TOC-GPX-track-import"></a>Import a GPX track

While viewing a map, press the button below on the upper right corner :

<p align="center">
<img src="doc/tuto/manage-tracks.jpg" width="300">
</p>

Then, a list of currently available tracks opens up: 

<p align="center">
<img src="doc/tuto/track-list.jpg" width="300">
</p>

Here you can:

* Import a new gpx files using the import button
* Manage track visibility
* Remove tracks by swiping them left or right

### <a name="TOC-GPX-recording"></a>GPX recording

It is possible to record your position and create a GPX file, to later import into a map or share
with other people.

From the "GPX Record" option menu, you get the following interface:

<p align="center">
<img src="doc/tuto/gpx-recording.jpg" width="300">
</p>

The recording can be started or stopped from the control panel. When recording, the location service
runs in the background. It continues even if TrekAdvisor is stopped or paused.
An indicator reports back the status of the location service. A panel shows the list of recordings.

From there, when selecting a track, there are two buttons at the bottom which enable you to :

* rename it
* import it into an existing map (press the import button at the bottom of the list, then choose the map)

To delete a recording, long-press on it. The panel transitions into a selection mode, where there is a
remove button at the bottom. To go back to selection mode, long-press again somewhere in the list.
