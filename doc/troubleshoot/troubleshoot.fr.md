# Que faire si...

* [Je n'arrive pas à me localiser sur ma carte](#je-narrive-pas-à-me-localiser-sur-ma-carte)
* [Au moment de créer une carte, j'ai un message qui me dit de vérifier ma connexion internet](#au-moment-de-créer-une-carte-jai-un-message-qui-me-dit-de-vérifier-ma-connexion-internet)
* [Mon enregistrement GPX s'arrête parfois tout seul](#mon-enregistrement-gpx-sarrête-parfois-tout-seul)
* [Des lignes droites apparaîssent sur mon enregistrement GPX](#des-lignes-droites-apparaîssent-sur-mon-enregistrement-gpx)
* [J'ai un autre problème](#jai-un-autre-problème)


## Je n'arrive pas à me localiser sur ma carte

Il peut y avoir plusieurs causes à ce problème. Il faut procéder par étapes :

1. Etes vous dans un endroit qui peut recevoir un signal gps ?\
Les milieux clos comme les appartements, les maisons etc.. rendent parfois difficile la réception 
d'un signal GPS (qui provient de sattelites).

2. L'application a-t-elle l'autorisation d'accéder à votre position ?\
Dans votre système Android, dans les paramètres, puis applications et pour TrekMe, vérifiez que 
dans les autorisations tout est bien activé (notamment la position).

3. La localisation est-elle activée sur le téléphone ?\
Bien que toutes les autorisations soient accordées à TrekMe, la localisation peut être désactivée 
sur l'appareil. Et dans ce cas, TrekMe ne pourra pas afficher votre position.


## Au moment de créer une carte, j'ai un message qui me dit de vérifier ma connexion internet

La philosophie de TrekMe est de télécharger une carte chez soi quand on a accès à internet, puis de 
faire la randonnée mais cette fois TrekMe ne nécessite aucune connexion.
Si vous confirmez que votre connexion internet fonctionne normalement, alors il peut y avoir un souci
soit avec les serveurs de l'IGN, soit avec l'un des services de TrekMe. Dans ce cas, contactez-moi
par mail à plr.devs@gmail.com

## Mon enregistrement GPX s'arrête parfois tout seul

Un enregistrement GPX est lancé sous forme de service Android, qui tourne en tâche de fond. 
Mais certains téléphones sont réglés de manière a limiter ces services, pour économiser la 
batterie (la tâche de fond est alors identifiée comme drainant la batterie, puis elle est stoppée).
Dans le cas d'un enregistrement GPX, il est normal de consommer un peu de batterie. Mais Android ne
fait pas de distinction, et arrête brutalement le service (et TrekMe avec).

Pour éviter ce problème, il faut aller dans les réglages du téléphone, dans "Batterie". Ensuite, il 
faut y trouver un menu "Optimisation de la batterie" ou similaire (cela diffère d'un téléphone a 
l'autre). Là on peut afficher la liste des applications "optimisées". Si TrekMe fait partie de cette liste, 
il faut autoriser son exécution en arrière plan en désactivant l'optimisation.

## Des lignes droites apparaîssent sur mon enregistrement GPX

Ce problème peut avoir deux causes :

1. L'optimisation de la batterie est active pour TrekMe. Dans ce cas, voir [Mon enregistrement GPX s'arrête parfois tout seul](#mon-enregistrement-gpx-sarrête-parfois-tout-seul).

2. L'autorisation de localisation pour TrekMe n'est pas au niveau suffisant. Elle doit être au niveau
"Toujours autoriser", et pas seulement quand l'appli est en cours d'utilisation.

## J'ai un autre problème

Votre problème ne figure pas parmi ceux cités plus haut. Dans ce cas, contactez-moi par mail à plr.devs@gmail.com

Je ferai tout mon possible pour vous aider.



