# Que faire si...

* [Je n'arrive pas à me localiser sur ma carte](#TOC-loc)
* [Au moment de créer une carte, j'ai un message qui me dit de vérifier ma connexion internet](#TOC-no-internet)
* [Mon enregistrement GPX s'arrête parfois tout seul](#TOC-record-gpx-stop)
* [J'ai un autre problème](#TOC-other)


## <a name="TOC-loc"></a>Je n'arrive pas à me localiser sur ma carte

Il peut y avoir plusieurs causes à ce problème. Il faut procéder par étapes :

1. Etes vous dans un endroit qui peut recevoir un signal gps ? 
  Les milieux clos comme les appartement, les maisons etc.. rendent parfois difficile la réception 
  d'un signal GPS.

2. L'application a-t-elle l'autorisation d'accéder à votre position ? 
  Dans votre système Android, dans les paramètres, puis applications et pour TrekMe, vérifiez que 
  dans les autorisations tout est bien activé (notamment la position).

3. La carte est-elle calibrée ?
  Dans la liste des cartes, sous le nom de votre carte, y a-t-il écrit "Calibrée" ? 
  Quand une carte a fini de se télécharger, elle est calibrée mais parfois ce n'est pas le cas. Vous 
  avez alors deux possibilités :
  
  * Calibrer votre carte. Un tutoriel sur ce point sera ajouté prochainement.
  * Re-télécharger votre carte

## <a name="TOC-quota-IGN"></a> Je voudrais savoir où j'en suis dans mon quota de téléchargement IGN

Connectez-vous à votre espace sur le site de l'IGN, puis cliquez sur "Récupérer mes clés d'accès aux 
services en ligne" depuis les liens rapides (sur la gauche).

Sous votre clé d'Api, cliquez sur "Statistiques de consommation", puis sur "Générer les statistiques".

Une page s'affiche alors avec tout en haut une période de début et de fin. Je vous recommande de 
changer l'année de la date de début en mettant l'année précédente, pour être sûr de couvrir toute 
l'année passée. 
Défilez la page vers le bas, laissez cochées toutes les ressources, et cochez la case "Inclure toutes
 les ressources ayant été utilisées dans la période choisie".

Enfin cliquez sur le bouton "CALCULER LES STATISTIQUES" tout en bas. Après quelques secondes, vous 
pouvez défiler la page vers le bas pour voir le résultat. Pour rappel, si vous avez souscrit à l'offre
 gratuite, vous avez droit à 2 000 000 de transactions par an (ce qui est énorme).

## <a name="TOC-no-internet"></a> Au moment de créer une carte, j'ai un message qui me dit de vérifier ma connexion internet

La philosophie de TrekMe est de télécharger une carte chez soi quand on a accès à internet, puis de 
faire la randonnée mais cette fois TrekMe ne nécessite aucune connexion.
Si vous confirmez que votre connexion internet fonctionne normalement, alors il peut y avoir un souci
soit avec les serveurs de l'IGN, soit avec l'un des services de TrekMe. Dans ce cas, contactez-moi
par mail à plr.devs@gmail.com

## <a name="#TOC-record-gpx-stop"></a> Mon enregistrement GPX s'arrête parfois tout seul

Un enregistrement GPX est lancé sous forme de service Android, qui tourne en tâche de fond. 
Mais certains téléphones sont réglés de manière a limiter ces tâches de fond, pour économiser la 
batterie (la tâche de fond est alors identifiée comme drainant la batterie, puis elle est stoppée).
Il faut aller dans les réglages du téléphone, dans "Batterie". Ensuite, il faut y trouver un menu 
"Optimisation de la batterie" ou similaire (cela diffère d'un téléphone a l'autre). Là on peut 
définir pour chaque application la limite ou non de son activité en arrière plan. 
Donc pour Trekme, il faut désactiver l'optimisation.

## <a name="TOC-other"></a> J'ai un autre problème

Votre problème ne figure pas parmi ceux cités plus haut. Dans ce cas, contactez-moi par mail à plr.devs@gmail.com

Je ferai tout mon possible pour vous aider.



