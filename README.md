# pismile-java

[![JitPack](https://jitpack.io/v/getpismile/pismile-java.svg)](https://jitpack.io/#getpismile/pismile-java) [![Tests](https://github.com/getpismile/pismile-java/actions/workflows/ci.yml/badge.svg)](https://github.com/getpismile/pismile-java/actions/workflows/ci.yml)

SDK serveur **pismile** pour Java (17 et plus) : encaissez avec le bouton **« Payer avec Alias PI-SPI »**, envoyez des
demandes de paiement à vos utilisateurs « Se connecter avec pismile », et vérifiez les **webhooks**.

- **Aucune dépendance** : fonctionne avec Spring Boot, Jakarta EE, Quarkus, Micronaut ou du Java simple.
- L'argent arrive **directement sur le compte de votre organisation**, avec votre accès participant PI-SPI.
- Le payeur valide chaque demande dans sa banque. Il peut **payer seul ou partager sur plusieurs comptes**.

> À utiliser **uniquement côté serveur** : le `client_secret` ne doit jamais aller dans un navigateur ni une appli mobile.

## Installation

Avec [JitPack](https://jitpack.io/#getpismile/pismile-java) :

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.getpismile</groupId>
    <artifactId>pismile-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle : `implementation("com.github.getpismile:pismile-java:0.1.0")` avec `maven("https://jitpack.io")`.

## Prérequis (console pismile, une fois)

1. **Modules** : Encaissement activé, **accès participant** PI-SPI vérifié (AFA-Pay, MTN…).
2. **Domaines** : votre domaine vérifié (l'adresse de retour du payeur doit y être).
3. **Applications** → votre application → **Paiements** : adresse du webhook, puis copiez le secret `whsec_…`.
4. Pour une application de production : organisation vérifiée.

## Le bouton « Payer avec Alias PI-SPI »

Le client clique, paie sur pismile.com (sans compte), puis revient sur votre site. Trois morceaux :

### 1. Le bouton (page commande ou produit)

```html
<script src="https://pismile.com/kit/v1/pismile-button.js" defer></script>
<pismile-button text="pay" href="/commandes/42/payer"></pismile-button>
```

### 2. La route du bouton : créer la page et y envoyer le client

```java
@Configuration
class PismileConfig {
    @Bean
    Pismile pismile() {
        return Pismile.depuisEnvironnement(); // PISMILE_CLIENT_ID, PISMILE_CLIENT_SECRET, PISMILE_WEBHOOK_SECRET
    }
}

@Controller
class PaiementController {

    private final Pismile pismile;
    private final CommandeService commandes;   // le vôtre

    PaiementController(Pismile pismile, CommandeService commandes) {
        this.pismile = pismile;
        this.commandes = commandes;
    }

    @GetMapping("/commandes/{id}/payer")
    String payer(@PathVariable long id) {
        Commande c = commandes.duClientConnecte(id);
        SessionPaiement page = pismile.sessions().creer(
                c.getTotalFcfa(),                                   // montant en F CFA
                "cmd-" + id,                                        // votre référence : la renvoyer renvoie la même page
                "https://votre-site.com/commandes/" + id + "/merci", // domaine vérifié
                "Commande " + id);                                  // motif affiché au payeur
        return "redirect:" + page.url();                            // https://pismile.com/payer/cs_…
    }
}
```

Au retour, l'adresse reçoit `?pismile_session=cs_…&statut=payee`. **Ce n'est pas une preuve** (n'importe qui peut
ouvrir cette adresse) : confirmez avec le webhook, ou relisez `pismile.sessions().lire(id)`.

### 3. Le webhook : la preuve de paiement

```java
@RestController
class PismileWebhook {

    private final Pismile pismile;
    private final CommandeService commandes;

    PismileWebhook(Pismile pismile, CommandeService commandes) {
        this.pismile = pismile;
        this.commandes = commandes;
    }

    @PostMapping("/webhooks/pismile")
    ResponseEntity<Void> recevoir(@RequestBody String corps,                       // corps BRUT
                                  @RequestHeader(Webhooks.ENTETE) String signature) {
        Evenement e;
        try {
            e = pismile.webhooks().verifier(corps, signature);
        } catch (PismileException refus) {
            return ResponseEntity.badRequest().build();
        }
        if (e.estSessionPayee()) {
            commandes.marquerPayee(e.reference());   // « cmd-42 » ; sans effet si déjà payée
        }
        return ResponseEntity.ok().build();
    }
}
```

Excluez `/webhooks/pismile` de la protection CSRF (appel de serveur à serveur) :
`http.csrf(c -> c.ignoringRequestMatchers("/webhooks/pismile"))`.

pismile réessaie un webhook non acquitté (2xx en moins de 10 s) : 1 min, 5 min, 30 min, 2 h, 6 h, 12 h, 24 h.
Un même événement peut arriver deux fois : dédoublonnez avec `e.id()`.

| Événement | Quand |
|---|---|
| `session.payee` | La page est entièrement payée : livrez |
| `session.expiree` | 30 min sans paiement complet ; `e.session().montantPaye()` indique un éventuel paiement partiel |
| `paiement.paye` · `paiement.refuse` · `paiement.annule` · `paiement.expire` | Demandes aux utilisateurs connectés (ci-dessous) |
| `test` | Bouton « Envoyer un test » de la console |

## Demander un paiement à un utilisateur connecté

Si vos utilisateurs se connectent avec « Se connecter avec pismile » et le scope `pismile:payment`, vous pouvez leur
envoyer une demande sans les rediriger (abonnement, facture…). Le `sub` est celui reçu à la connexion.

```java
Paiement p = pismile.paiements().creer(sub, 5000, "facture-2026-10", "Abonnement octobre");
p.statut();   // EN_ATTENTE → PAYE, REFUSE, ANNULE ou EXPIRE (webhooks paiement.*)
```

## Erreurs

Toutes les erreurs sont des `PismileException` : `getCode()` (stable), `getStatut()` (HTTP), `getMessage()` (lisible).

| Code | Que faire |
|---|---|
| `authentification` | client_id ou client_secret incorrect |
| `retour_domaine_non_verifie` | ajoutez et vérifiez votre domaine (console → Domaines) |
| `reference_deja_utilisee` | référence déjà prise par un autre montant |
| `module_encaissement_requis` · `acces_participant_non_verifie` · `organisation_non_verifiee` | voir la console |
| `autorisation_absente` | (demandes) reconnecter l'utilisateur avec `pismile:payment` |
| `reseau` · `delai_depasse` | pismile injoignable : réessayez avec la même référence |

Le SDK réessaie déjà de lui-même après une coupure réseau ou une erreur serveur temporaire : sans risque, la
référence empêche tout double paiement.

## Configuration détaillée

```java
Pismile pismile = Pismile.builder()
        .clientId("...")
        .clientSecret("...")
        .webhookSecret("whsec_...")
        .issuer("https://pismile.com")
        .delai(Duration.ofSeconds(15))
        .essais(2)
        .build();
```

## Documentation

Guides complets (liens de paiement, bouton, webhooks, référence de l'API) : [pismile.com/docs/paiements](https://pismile.com/docs/paiements/vue-ensemble).

## Développement

```bash
mvn verify
```

## Licence

MIT
