import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.npcs.Ally;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Bullet;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.socket.data.receive_data.Item;

import javax.swing.text.Utilities;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static jsclub.codefest.sdk.algorithm.PathUtils.*;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "148810";
    private static final String PLAYER_NAME = "obslearn2code";
    private static final String SECRET_KEY = "sk-e0C0rY8MSDy3S1KJAuEvuQ:oC31IGg1xcdFU3qtR4o0Qy6hPyV-lX-EymaC9STy2i6n9NdaQAjp2hMQZy-rwzVNpVbta76POud9nC9EED1TFA";


    public static void main(String[] args) throws IOException {
        boolean findingGun = false;
        boolean findingHealth = false;
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        int timeShot = -1000;
        Emitter.Listener onMapUpdate = new MapUpdateListener(hero);

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }

}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;
    // Global variable to track the last time hero shoots
    private long lastShootTime = 0;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();

            // Search for information on the map
            GameContext ctx = new GameContext(gameMap, player);



            if (player == null || player.getHealth() == 0) {
                System.out.println("Player is dead or data is not available.");
                return;
            }


            if (shouldHeal(ctx)){
                heal(ctx);
                return ;
            }

            if (shouldLootGun(ctx)){
                lootGun(ctx);
                return ;
            }
            if (shouldLootMelee(ctx)){
                lootMelee(ctx);
                return ;
            }
            if(shouldBreakChest(ctx)){
                breakChest(ctx);
                return ;
            }
            if(shouldAttack(ctx)){
                attack(ctx);
                return ;
            }


            // End code here
            System.out.println("Last shoot time: " + lastShootTime);

        } catch (Exception e) {
            System.err.println("Critical error in call method: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Node> getNodesToAvoid(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        nodes.addAll(gameMap.getObstaclesByTag("TRAP"));
        nodes.addAll(gameMap.getObstaclesByTag("DESTRUCTIBLE"));
        return nodes;
    }

    private String findPathToGun(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Weapon nearestGun = getNearestGun(gameMap, player);
        if (nearestGun == null) return null;
        return getShortestPath(gameMap, nodesToAvoid, player, nearestGun, false);
    }


    // Private Function
    private String findPathToMelee(GameMap gameMap, List<Node> nodes, Player player) {
        Weapon nearestMelee = getNearestMelee(gameMap, player);
        if (nearestMelee == null) return null;
        return getShortestPath(gameMap, nodes, player, nearestMelee, false);
    }


    // Find the nearest object
    private Weapon getNearestGun(GameMap gameMap, Player player) {
        List<Weapon> guns = gameMap.getAllGun();
        Weapon nearestGun = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon gun : guns) {
            if (checkInsideSafeArea(gun, gameMap.getSafeZone(), gameMap.getMapSize())) {
                if (hero.getInventory().getGun() == null || gun.getDamage() > hero.getInventory().getGun().getDamage()) {
                    double distance = distance(player, gun);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestGun = gun;
                    }
                }
            }
        }
        return nearestGun;
    }

    private Player getNearestBot(GameMap gameMap, Player player) {
        List<Player> enemies = gameMap.getOtherPlayerInfo();
        Player nearestBot = null;
        double minDistance = Double.MAX_VALUE;
        for (Player e : enemies) {
            if (checkInsideSafeArea(e, gameMap.getSafeZone(), gameMap.getMapSize())) {
                double distance = distance(player, e);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestBot = e;
                }
            }
        }
        return nearestBot;
    }

    private Obstacle getNearestChest(GameMap gameMap, Player player) {
        List<Obstacle> chests = gameMap.getObstaclesByTag("DESTRUCTIBLE");
        Obstacle nearestChest = null;
        double minDistance = Double.MAX_VALUE;
        for (Obstacle chest : chests) {
            if (checkInsideSafeArea(chest, gameMap.getSafeZone(), gameMap.getMapSize())) {
                double distance = distance(player, chest);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestChest = chest;
                }
            }
        }
        return nearestChest;
    }

    private Ally getNearestAlliance(GameMap gameMap, Player player) {
        List<Ally> allys = gameMap.getListAllies();
        Ally nearestAlliance = null;
        double minDistance = Double.MAX_VALUE;
        for (Ally ally : allys) {
            if (checkInsideSafeArea(ally, gameMap.getSafeZone(), gameMap.getMapSize())) {
                double distance = distance(player, ally);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestAlliance = ally;
                }
            }
        }
        return nearestAlliance;
    }

    private boolean CheckdistanceToAlly(GameMap gameMap, Player player) {
        Ally ally = getNearestAlliance(gameMap, player);
        return ally != null && distance(ally, player) <= 7;
    }

    private Armor getNearestArmor(GameMap gameMap, Player player) {
        List<Armor> armors = gameMap.getListArmors();
        Armor nearestArmor = null;
        double minDistance = Double.MAX_VALUE;
        for (Armor armor : armors) {
            if (!checkInsideSafeArea(armor, gameMap.getSafeZone(), gameMap.getMapSize())) {
                continue;
            }
            if (hero.getInventory().getHelmet() == null || hero.getInventory().getArmor() == null) {
                double distance = distance(player, armor);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestArmor = armor;
                }
            } else {
                if (hero.getInventory().getHelmet().getId().equals("WOODEN_HELMET") || hero.getInventory().getArmor().getId().equals("MAGIC_ARMOR")) {
                    if (armor.getDamageReduce() > hero.getInventory().getArmor().getDamageReduce() || (armor.getDamageReduce() > hero.getInventory().getHelmet().getDamageReduce())) {
                        double distance = distance(player, armor);
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestArmor = armor;
                        }
                    }
                }
            }
        }
        return nearestArmor;
    }

    private SupportItem getNearestSupportItem(GameMap gameMap, Player player) {
        List<SupportItem> supportItems = gameMap.getListSupportItems();
        SupportItem nearestSupportItem = null;
        SupportItem minSupportItemHealHP = MinSupportItem(player);
        double minDistance = Double.MAX_VALUE;
        for (SupportItem supportItem : supportItems) {
            if (checkInsideSafeArea(supportItem, gameMap.getSafeZone(), gameMap.getMapSize())) {
                if (hero.getInventory().getListSupportItem().size() < 4 || supportItem.getHealingHP() > minSupportItemHealHP.getHealingHP()) {
                    double distance = distance(player, supportItem);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestSupportItem = supportItem;
                    }
                }
            }
        }
        return nearestSupportItem;
    }

    private SupportItem MinSupportItem(Player player) {
        SupportItem min = null;
        for (SupportItem sp : hero.getInventory().getListSupportItem()) {
            if (min == null) {
                min = sp;
            } else if (min.getHealingHP() > sp.getHealingHP()) {
                min = sp;
            }
        }
        return min;
    }

    // Get nearest melee
    private Weapon getNearestMelee(GameMap gameMap, Player player) {
        List<Weapon> Melees = gameMap.getAllMelee();
        Weapon nearestMelee = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon melee : Melees) {
            if (checkInsideSafeArea(melee, gameMap.getSafeZone(), gameMap.getMapSize())) {
                if (hero.getInventory().getMelee() == null || melee.getDamage() > hero.getInventory().getMelee().getDamage() || melee.getHitPoints() > hero.getInventory().getMelee().getHitPoints()) {
                    double distance = distance(player, melee);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestMelee = melee;
                    }
                }
            }
        }
        return nearestMelee;
    }

    //     Get Nearest Throwable  Weapon
    private Weapon getNearestThrowable(GameMap gameMap, Player player) {
        List<Weapon> throwables = gameMap.getAllThrowable();
        Weapon nearestThrowable = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon throwable : throwables) {
            if (checkInsideSafeArea(throwable, gameMap.getSafeZone(), gameMap.getMapSize())) {
                if (hero.getInventory().getThrowable() == null || throwable.getDamage() > hero.getInventory().getThrowable().getDamage() || throwable.getPickupPoints() > hero.getInventory().getThrowable().getPickupPoints()) {
                    double distance = distance(player, throwable);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestThrowable = throwable;
                    }
                }
            }
        }
        return nearestThrowable;
    }

    private Weapon getNearestSpecial(GameMap gameMap, Player player) {
        List<Weapon> specials = gameMap.getAllSpecial();
        Weapon nearestSpecial = null;
        double minDistance = Double.MAX_VALUE;
        for (Weapon special : specials) {
            if (checkInsideSafeArea(special, gameMap.getSafeZone(), gameMap.getMapSize())) {
                if (hero.getInventory().getSpecial() == null || hero.getInventory().getSpecial().getDamage() < special.getDamage()) {
                    double distance = distance(player, special);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestSpecial = special;
                    }
                }
            }
        }
        return nearestSpecial;
    }

    // Return gun or melee nearest. If not return null
    private Weapon getNearestWeapon(GameMap gameMap, Player player) {
        // Check gun first
        if (hero.getInventory().getGun() == null) {
            Weapon gun = getNearestGun(gameMap, player);
            if (gun != null) {
                return gun;
            }
        }

        // Check melee weapons
        if (hero.getInventory().getMelee().getId().equals("HAND")) {
            Weapon melee = getNearestMelee(gameMap, player);
            if (melee != null) {
                return melee;
            }
        }

        // If all types are owned or no weapons found
        return null;
    }

    // Get Nearest Throwable Weapon if distance is 4
    private Weapon getNearestThrowableInRange(GameMap gameMap, Player player) {
        List<Weapon> throwables = gameMap.getAllThrowable();
        Weapon nearestThrowable = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon throwable : throwables) {
            if (checkInsideSafeArea(throwable, gameMap.getSafeZone(), gameMap.getMapSize())) {
                double distance = distance(player, throwable);
                if (distance < minDistance && distance < 4) {
                    minDistance = distance;
                    nearestThrowable = throwable;
                }
            }
        }
        return nearestThrowable;
    }

    // Get the nearest armor if the distance is 4
    private Armor getNearestArmorInRange(GameMap gameMap, Player player) {
        List<Armor> armors = gameMap.getListArmors();
        Armor nearestArmor = null;
        double minDistance = Double.MAX_VALUE;
        for (Armor armor : armors) {
            if (!checkInsideSafeArea(armor, gameMap.getSafeZone(), gameMap.getMapSize())) {
                continue;
            }
            double distance = distance(player, armor);
            if (distance < minDistance && distance < 4) {
                minDistance = distance;
                nearestArmor = armor;
            }
        }
        return nearestArmor;
    }


    private Weapon checkDistanceAndUpdate(Player player, Weapon newWeapon, Weapon currentWeapon, double currentMinDistance) {
        if (newWeapon != null) {
            double newDistance = distance(player, newWeapon);
            if (currentWeapon == null || newDistance < currentMinDistance) {
                return newWeapon;
            }
        }
        return currentWeapon;
    }


    // Find Path to the object
    private String findPathToEnemy(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Player e = getNearestBot(gameMap, player);
        if (e == null) return null;
        return getShortestPath(gameMap, nodesToAvoid, player, e, false);
    }

    private String findPathToChest(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Obstacle nearestChest = getNearestChest(gameMap, player);
        if (nearestChest == null) {
            System.out.println("No nearest chest found.");
            return null;
        }
        return getShortestPath(gameMap, nodesToAvoid, player, nearestChest, false);
    }

    private String findPathToHealth(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Ally nearestHealth = getNearestAlliance(gameMap, player);
        if (nearestHealth == null) {
            System.out.println("No nearest health found.");
            return null;
        }
        return getShortestPath(gameMap, nodesToAvoid, player, nearestHealth, false);
    }

    private String findPathToArmor(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Armor nearestArmor = getNearestArmor(gameMap, player);
        if (nearestArmor == null) {
            System.out.println("No nearest armor found.");
            return null;
        }
        return getShortestPath(gameMap, nodesToAvoid, player, nearestArmor, false);
    }

    private String findPathToSupportItem(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        SupportItem nearestSupport = getNearestSupportItem(gameMap, player);
        if (nearestSupport == null) {
            System.out.println("No nearest armor found.");
            return null;
        }
        return getShortestPath(gameMap, nodesToAvoid, player, nearestSupport, false);
    }

    private String findPathToWeapon(GameMap gameMap, List<Node> nodesToAvooid, Player player) {
        Weapon nearestWeapon = getNearestWeapon(gameMap, player);
        if (nearestWeapon == null) {
            System.out.println("No nearest weapon found");
            return null;
        }
        return getShortestPath(gameMap, nodesToAvooid, player, nearestWeapon, false);
    }

    private void handleTheWeapon(GameContext ctx) throws IOException {
        String weapon = findPathToWeapon(ctx.gameMap, ctx.nodesToAvoid, ctx.player);
        System.out.println("pick weapon");
        if (weapon != null) {
            if (weapon.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(weapon);
            }
        }
    }

    // Take Nearest object
    private void attackNearestBot(GameContext ctx) throws IOException {
        Player e = ctx.nearestBot;
        if (e == null) return;

        String pathToBot = getShortestPath(ctx.gameMap, ctx.nodesToAvoid, ctx.player, e, false);
        if (pathToBot == null) return;

        int distanceX = Math.abs(ctx.player.getX() - e.getX());
        int distanceY = Math.abs(ctx.player.getY() - e.getY());
        int gunRange = hero.getInventory().getGun().getRange()[1];
        int currentStep = ctx.gameMap.getStepNumber();
        double cooldown = hero.getInventory().getGun().getCooldown();
        boolean canShoot = (currentStep - lastShootTime >= cooldown);

        // Bắn nếu đủ cooldown và trong tầm
        if (ctx.player.getX() == e.getX() && distanceY <= gunRange && canShoot) {
            hero.shoot(ctx.player.getY() > e.getY() ? "d" : "u");
            lastShootTime = currentStep;
            return;
        }

        if (ctx.player.getY() == e.getY() && distanceX <= gunRange && canShoot) {
            hero.shoot(ctx.player.getX() < e.getX() ? "r" : "l");
            lastShootTime = currentStep;
            return;
        }

        // Nếu pathToBot rỗng (đã sát enemy) => melee
        if (pathToBot.length() == 1) {
            String dir = directionTo(ctx.player, e);
            hero.attack(dir);
            System.out.println("Attack " + dir);
            return;
        }

        // Nếu không bắn, không attack => di chuyển
        hero.move(pathToBot);
        System.out.println("Move to bot: " + pathToBot);
    }

    private String directionTo(Player player, Player e) {
        int dx = e.getX() - player.getX();
        int dy = e.getY() - player.getY();

        if (dx == 0) {
            if (dy < 0) {
                return "d";
            } else if (dy > 0) {
                return "u";
            }
        }
        if (dy == 0) {
            if (dx < 0) {
                return "l";
            } else if (dx > 0) {
                return "r";
            }
        }

        // Nếu không cùng hàng/cột, không trả hướng tấn công
        return "";
    }

    private void pickNearestChest(GameContext ctx) throws IOException {
        String pathToChest = getShortestPath(ctx.gameMap, ctx.nodesToAvoid, ctx.player, ctx.nearestChest, false);
        if (pathToChest == null) {
            System.out.println("No nearest chest found.");
            return;
        }
        System.out.println("Chest found: " + ctx.nearestChest.getX() + " " + ctx.nearestChest.getY());
        if (pathToChest.equals("u") || pathToChest.equals("r") || pathToChest.equals("l") || pathToChest.equals("d")) {
            hero.attack(pathToChest);
        } else {
            hero.move(pathToChest);
        }
    }


    private void heal(GameContext ctx) throws IOException {
        System.out.println("Getting nearest health.");

        SupportItem minItem = MinSupportItem(ctx.player);

        if (hero.getInventory().getListSupportItem() != null && !hero.getInventory().getListSupportItem().isEmpty() && minItem != null) {
            float missingHP = 100 - ctx.player.getHealth();
            if (missingHP >= minItem.getHealingHP()) {
                System.out.println("Using support item to heal.");
                hero.useItem(minItem.getId());
            }
        }
    }


    private void handleSearchForGun(GameContext ctx) throws IOException {
        if (ctx.nearestGun == null) {
            System.out.println("No gun found. Finding new one.");
            return;
        }
        String pathToGun = getShortestPath(ctx.gameMap,ctx.nodesToAvoid,ctx.player,ctx.nearestGun,false);
        if (pathToGun != null) {
            if (pathToGun.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(pathToGun);
            }
        }


    }

    private void pickNearestArmor(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No armor found. Searching for a armor.");
        String pathToArmor = findPathToArmor(gameMap, nodesToAvoid, player);
        if (pathToArmor != null) {
            if (pathToArmor.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(pathToArmor);
            }
        }
    }

    private void pickNearestToSupportItem(GameMap gameMap, Player player, List<Node> nodes) throws IOException {
        System.out.println("Searching for supportItem");
        String supportItem = findPathToSupportItem(gameMap, getNodesToAvoid(gameMap), player);
        if (supportItem != null) {
            if (supportItem.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(supportItem);
            }
        }
    }

    private void pickNearestSpecial(GameMap gameMap, Player player, List<Node> nodes) throws IOException {
        Weapon special = getNearestSpecial(gameMap, player);
        String pathToGetSpecial = getShortestPath(gameMap, nodes, player, special, false);
        if (pathToGetSpecial != null) {
            if (pathToGetSpecial.isEmpty()) {
                if (hero.getInventory().getSpecial() == null) {
                    hero.pickupItem();
                } else {
                    hero.pickupItem();
                    hero.revokeItem(hero.getInventory().getSpecial().getId());
                }
            } else {
                hero.move(pathToGetSpecial);
            }
        }
    }

    private void pickNearestMelee(GameContext ctx) throws IOException {
        if (ctx.nearestMelee == null) {
            System.out.println("No gun found. Finding new one.");
            return;
        }
        String pathToMelee = getShortestPath(ctx.gameMap,ctx.nodesToAvoid,ctx.player,ctx.nearestMelee,false);
        if (pathToMelee != null) {
            if (pathToMelee.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(pathToMelee);
            }
        }

    }

    private void pickNearestThrowable(GameMap gameMap, Player player, List<Node> nodes) throws IOException {
        Weapon throwable = getNearestThrowable(gameMap, player);
        String pathToGetThrowable = getShortestPath(gameMap, getNodesToAvoid(gameMap), player, throwable, false);
        if (pathToGetThrowable != null) {
            if (pathToGetThrowable.isEmpty()) {
                if (hero.getInventory().getThrowable() == null) {
                    hero.pickupItem();
                } else {
                    hero.pickupItem();
                    hero.revokeItem(hero.getInventory().getThrowable().getId());
                }
            } else {
                hero.move(pathToGetThrowable);
            }
        }


    }

    private boolean checkItemAround(GameMap gameMap, Player player) {
        Weapon gun = getNearestGun(gameMap, player);
        Weapon melee = getNearestMelee(gameMap, player);
        Weapon special = getNearestSpecial(gameMap, player);
        Node chest = getNearestChest(gameMap, player);
        Armor helmet = getNearestArmor(gameMap, player);
        SupportItem supportItem = getNearestSupportItem(gameMap, player);

        System.out.println("check Item Around");

        if (gun != null) System.out.println("dis gun: " + distance(gun, player));
        if (melee != null) System.out.println("dis melee: " + distance(melee, player));
        if (special != null) System.out.println("dis special: " + distance(special, player));
        if (chest != null) System.out.println("dis chest: " + distance(chest, player));
        if (helmet != null) System.out.println("dis helmet: " + distance(helmet, player));
        if (supportItem != null) System.out.println("dis support: " + distance(supportItem, player));

        return (gun != null && distance(gun, player) <= 3)
                || (melee != null && distance(melee, player) <= 3)
                || (special != null && distance(special, player) <= 3)
                || (chest != null && distance(chest, player) <= 3)
                || (helmet != null && distance(helmet, player) <= 3 &&
                (hero.getInventory().getArmor() == null || hero.getInventory().getHelmet() == null))
                || (supportItem != null && distance(supportItem, player) <= 3 &&
                hero.getInventory().getListSupportItem().size() <= 3);
    }

//    private void getItemAround(GameMap gameMap, Player player) throws IOException {
//        System.out.println("GO around to get Armor and SpItems and enhance Weapon");
//
//        Weapon gun = getNearestGun(gameMap, player);
//        Weapon melee = getNearestMelee(gameMap, player);
//        Weapon special = getNearestSpecial(gameMap, player);
//        Weapon throwable = getNearestThrowable(gameMap, player);
//        Node chest = getNearestChest(gameMap, player);
//        Armor helmet = getNearestArmor(gameMap, player);
//        SupportItem supportItem = getNearestSupportItem(gameMap, player);
//        System.out.println("see some Items around");
//
//        if (gun != null && PathUtils.distance(gun, player) <= 3) {
//            System.out.println("go to get gun:");
//            String pathToGun = findPathToGun(gameMap, getNodesToAvoid(gameMap), player);
//            if (pathToGun != null) {
//                if (pathToGun.isEmpty()) {
//                    System.out.println("go pick gun");
//                    if (hero.getInventory().getGun() == null) {
//                        System.out.println("just pick");
//                        hero.pickupItem();
//                    } else {
//                        System.out.println("thrown weapon to pick a new gun");
//                        hero.pickupItem();
//                        hero.revokeItem(hero.getInventory().getGun().getId());
//                    }
//                } else {
//                    hero.move(pathToGun);
//                }
//            }
//        } else if (melee != null && PathUtils.distance(melee, player) <= 3) {
//            System.out.println("Pick melee");
//            String pathToGetMelee = findPathToMelee(gameMap, getNodesToAvoid(gameMap), player);
//            if (pathToGetMelee != null) {
//                if (pathToGetMelee.isEmpty()) {
//                    if (hero.getInventory().getMelee().getId().equals("HAND")) {
//                        System.out.println("dont have melee and pick");
//                        hero.pickupItem();
//                    } else {
//                        hero.pickupItem();
//                        System.out.println("have melee and change to pick");
//                        hero.revokeItem(hero.getInventory().getMelee().getId());
//                    }
//                } else {
//                    hero.move(pathToGetMelee);
//                }
//            }
//        } else if (supportItem != null && PathUtils.distance(supportItem, player) <= 3 &&
//                hero.getInventory().getListSupportItem().size() <= 3) {
//            System.out.println("pick support item");
//            pickNearestToSupportItem(gameMap, player, getNodesToAvoid(gameMap));
//        } else if (throwable != null && PathUtils.distance(throwable, player) <= 3) {
//            System.out.println("pick throwable");
//            pickNearestThrowable(gameMap, player, getNodesToAvoid(gameMap));
//        } else if (chest != null && PathUtils.distance(chest, player) <= 3) {
//            System.out.println("pick chest");
//            pickNearestChest(gameMap, player, getNodesToAvoid(gameMap));
//        } else if (special != null && PathUtils.distance(special, player) <= 3) {
//            System.out.println("Pick special weapon");
//            pickNearestSpecial(gameMap, player, getNodesToAvoid(gameMap));
//        } else if (helmet != null && PathUtils.distance(helmet, player) <= 3 &&
//                (hero.getInventory().getArmor() == null || hero.getInventory().getHelmet() == null)) {
//            System.out.println("pick helmet");
//            pickNearestArmor(gameMap, player, getNodesToAvoid(gameMap));
//        }
//    }

    public class GameContext {
        public GameMap gameMap;
        public Player player;
        public List<Node> nodesToAvoid;
        public int mapSize;
        public int stepNumber;

        public Weapon nearestGun;
        public Weapon nearestMelee;
        public Obstacle nearestChest;
        public Armor nearestArmor;
        public Armor nearestHelmet;
        public Weapon nearestThrowable;
        public Player nearestBot;
        public SupportItem nearestHealth;

        public List<Bullet> bullets;
        public List<Player> enemies;
        public List<Item> items;

        public List<Node> stuffs;

        // Constructor
        public GameContext(GameMap gameMap, Player player) {
            this.gameMap = gameMap;
            this.player = player;
            this.mapSize = gameMap.getMapSize();
            this.stepNumber = gameMap.getStepNumber();
            this.nodesToAvoid = getNodesToAvoid(gameMap);
            this.bullets = gameMap.getListBullets();


            // Get info about things in the map
            stuffs.addAll(gameMap.getAllThrowable());
            stuffs.addAll(gameMap.getAllSpecial());
            stuffs.addAll(gameMap.getListArmors());
            stuffs.addAll(gameMap.getListSupportItems());
            stuffs.sort(Comparator.comparingInt(p -> distance(p, this.player)));


            // Cache nearest items
            this.nearestGun = getNearestGun(gameMap, player);
            this.nearestMelee = getNearestMelee(gameMap, player);
            this.nearestChest = getNearestChest(gameMap, player);
            this.nearestArmor = getNearestArmorInRange(gameMap, player);
            this.nearestThrowable = getNearestThrowableInRange(gameMap, player);
            this.nearestBot = getNearestBot(gameMap, player);
            this.nearestHealth = getNearestSupportItem(gameMap, player);
        }
    }

    private boolean shouldHeal(GameContext ctx) {
        return ctx.player.getHealth() < 40 && hero.getInventory().getSpecial() != null;
    }
//    private void heal(GameContext ctx) throws IOException {
//        hero.move(getShortestPath(ctx.gameMap,ctx.nodesToAvoid,ctx.player,ctx.nearestHealth,false));
//    }

    private boolean shouldLootGun(GameContext ctx) {
        return hero.getInventory().getGun() == null && ctx.nearestGun != null;
    }
    private void lootGun(GameContext ctx) throws IOException{
        handleSearchForGun(ctx);
    }

    private boolean shouldLootMelee(GameContext ctx) {
        return hero.getInventory().getMelee().getId().equals("HAND")  && ctx.nearestMelee != null;
    }
    private void lootMelee(GameContext ctx) throws IOException{
        pickNearestMelee(ctx);
    }
    private boolean shouldLootStuff(GameContext ctx) {
        int i = 0;
        while (i < ctx.stuffs.size()) {
            Node n = ctx.stuffs.get(i);
            if (distance(ctx.player, n) <= 3 && shouldLootThis(n, ctx)) {
                return true; // Dừng ngay khi tìm thấy
            }
            i++;
        }
        return false; // Không tìm thấy node nào thỏa điều kiện
    }
    private boolean shouldLootThis(Node node, GameContext ctx) {
        Element element = ctx.gameMap.getElementByIndex(node.getX(), node.getY());

        if (element == null) return false; // an toàn

        switch (element.getType()) {
            case PLAYER:
            case ENEMY:
            case ALLY:
            case CHEST:
            case TRAP:
            case INDESTRUCTIBLE:
            case ROAD:
            case BULLET:
                return false; // không loot

            case GUN:
                // Loot nếu chưa có gun
                if (hero.getInventory().getGun() == null) return true;
                // Loot nếu gun mới mạnh hơn (nếu bạn muốn so damage)
                if (ctx.nearestGun != null && ctx.nearestGun.getDamage() > hero.getInventory().getGun().getDamage()) {
                    return true;
                }
                return false;

            case MELEE:
                if (hero.getInventory().getMelee() == null) return true;
                if (hero.getInventory().getMelee().getId().equals("HAND")) return true;
                // So sánh damage nếu muốn:
                if (ctx.nearestMelee != null && ctx.nearestMelee.getDamage() > hero.getInventory().getMelee().getDamage()) {
                    return true;
                }
                return false;

            case THROWABLE:
                if (hero.getInventory().getThrowable() == null) return true;
                if (ctx.nearestThrowable != null && ctx.nearestThrowable.getDamage() > hero.getInventory().getThrowable().getDamage()) {
                    return true;
                }
                return false;

            case SPECIAL:
                // Nếu bạn muốn auto loot SPECIAL, return true;
                return true;

            case HEALING_ITEM:
            case SUPPORT_ITEM:
                // Loot nếu còn slot support item
                return hero.getInventory().getListSupportItem().size() < 3;

            case ARMOR:
                if (hero.getInventory().getArmor() == null) return true;
                if (ctx.nearestArmor != null && ctx.nearestArmor.getDamageReduce() > hero.getInventory().getArmor().getDamageReduce()) {
                    return true;
                }
                return false;

            case HELMET:
                if (hero.getInventory().getHelmet() == null) return true;
                if (ctx.nearestHelmet != null && ctx.nearestHelmet.getDamageReduce() > hero.getInventory().getHelmet().getDamageReduce()) {
                    return true;
                }
                return false;

            default:
                throw new IllegalStateException("Unexpected ElementType: " + element.getType());
        }
    }

    private boolean shouldBreakChest(GameContext ctx) {
        if (ctx.nearestChest == null)
            return false;
        if (hero.getInventory().getMelee().getId().equals("HAND"))
            return true;
        return true;
    }
    private void breakChest(GameContext ctx) throws IOException{
        System.out.println("Chest");
        pickNearestChest(ctx);
    }

    private boolean shouldAttack(GameContext ctx){
        return ctx.nearestBot != null && hero.getInventory().getMelee() != null && hero.getInventory().getGun()  != null;
    }

    private void attack(GameContext ctx) throws IOException{
        System.out.println("Attacking");
        attackNearestBot(ctx);
    }






}