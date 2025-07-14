import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.npcs.Ally;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Bullet;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.socket.data.receive_data.Item;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static jsclub.codefest.sdk.algorithm.PathUtils.*;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "102415";
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
            if (checkItemAround(ctx)){
                getItemAround(ctx);
                return;
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
            if (checkInsideSafeArea(e, gameMap.getSafeZone(), gameMap.getMapSize()) && e.getHealth() > 0) {
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
            if (hero.getInventory().getSpecial() != null) {
                hero.useSpecial(pathToBot);
            }
            String dir = directionTo(ctx.player, e);
            hero.attack(dir);
            System.out.println("Attack " + dir);
            return;
        }

        // Nếu không bắn, không attack => di chuyển
        hero.move(pathToBot);
        System.out.println("Move to bot: " + pathToBot);
    }

    private void attackNearestBotWithThrowableAndGun(GameContext ctx) throws IOException {
        Player enemy = ctx.nearestBot;
        if (enemy == null) return;

        int playerX = ctx.player.getX();
        int playerY = ctx.player.getY();
        int enemyX = enemy.getX();
        int enemyY = enemy.getY();

        int distanceX = Math.abs(playerX - enemyX);
        int distanceY = Math.abs(playerY - enemyY);
        int manhattanDistance = distanceX + distanceY;

        int currentStep = ctx.gameMap.getStepNumber();

        // --- Ưu tiên ném throwable trước ---
        if (hero.getInventory().getThrowable() != null) {
            int throwRange = hero.getInventory().getThrowable().getRange()[1] + hero.getInventory().getThrowable().getExplodeRange();
            if (distance(ctx.player,enemy) <= throwRange) {
                String throwDirection = getBestThrowableDirection(ctx, enemy);
                hero.throwItem(throwDirection);
                System.out.println("[BOT] Throwing throwable at enemy in direction: " + throwDirection);
                return;
            }
        }

        // --- Nếu không thể ném, xét bắn gun ---
        if (hero.getInventory().getGun() != null) {
            int gunRange = hero.getInventory().getGun().getRange()[1];
            double cooldown = hero.getInventory().getGun().getCooldown();
            boolean canShoot = (currentStep - lastShootTime >= cooldown);

            if (canShoot) {
                if (playerX == enemyX && distanceY <= gunRange) {
                    hero.shoot(playerY > enemyY ? "d" : "u");
                    lastShootTime = currentStep;
                    System.out.println("[BOT] Shooting vertically at enemy.");
                    return;
                }

                if (playerY == enemyY && distanceX <= gunRange) {
                    hero.shoot(playerX < enemyX ? "r" : "l");
                    lastShootTime = currentStep;
                    System.out.println("[BOT] Shooting horizontally at enemy.");
                    return;
                }
            }
        }

        // --- Nếu không ném hoặc bắn được, tìm vị trí tối ưu để giữ khoảng cách ---
        String pathToOptimalPosition = findPositionForRangedAttack(ctx, enemy);
        if (pathToOptimalPosition != null && !pathToOptimalPosition.isEmpty()) {
            hero.move(pathToOptimalPosition);
            System.out.println("[BOT] Moving to optimal ranged attack position: " + pathToOptimalPosition);
        } else {
            hero.move(PathUtils.getShortestPath(ctx.gameMap,ctx.nodesToAvoid,ctx.player,ctx.nearestBot,false));
            System.out.println("[BOT] Holding position, waiting for throwable/gun cooldown.");
        }
    }
    private String getBestThrowableDirection(GameContext ctx, Player enemy) {
        Weapon throwable = hero.getInventory().getThrowable();
        int throwRange = throwable.getRange()[1]; // tầm ném tối đa
        int explosionRadius = throwable.getExplodeRange(); // bán kính nổ

        Node botNode = new Node(ctx.player.getX(), ctx.player.getY());
        Node enemyNode = new Node(enemy.getX(), enemy.getY());

        String[] directions = {"r", "l", "d", "u"};
        int[][] deltas = {{1,0}, {-1,0}, {0,-1}, {0,1}}; // r, l, u, d

        for (int d = 0; d < 4; d++) {
            int dx = deltas[d][0];
            int dy = deltas[d][1];

            for (int step = 1; step <= throwRange; step++) {
                int throwX = botNode.getX() + dx * step;
                int throwY = botNode.getY() + dy * step;

                if (!isValidPosition(throwX, throwY, ctx.gameMap)) {
                    break; // không thể ném xa hơn hướng này
                }

                // Kiểm tra enemy có nằm trong phạm vi nổ quanh vị trí ném không
                if (Math.abs(throwX - enemyNode.getX()) + Math.abs(throwY - enemyNode.getY()) <= explosionRadius) {
                    return directions[d];
                }
            }
        }

        return null; // không tìm được hướng ném hợp lý
    }

    // Hàm hỗ trợ:
    private boolean isValidPosition(int x, int y, GameMap gameMap) {
        return x >= 0 && y >= 0 && x < gameMap.getMapSize() && y < gameMap.getMapSize();
    }

    private String findPositionForRangedAttack(GameContext ctx, Player enemy) {
        List<Node> potentialPositions = getNearbyNodes(ctx.player, hero.getInventory().getGun().getRange()[1], ctx.gameMap);
        for (Node pos : potentialPositions) {
            if (isInLineWith(pos, enemy) &&
                    distance(pos, enemy) <= hero.getInventory().getGun().getRange()[1]) {
                List<Node> tmpAvoid = ctx.nodesToAvoid;
                String path = PathUtils.getShortestPath(ctx.gameMap, tmpAvoid, ctx.player, pos, false);
                if (path != null && !path.isEmpty()) return path;
            }
        }
        return null;
    }
    private List<Node> getNearbyNodes(Node center, int radius, GameMap gameMap) {
        List<Node> nearbyNodes = new ArrayList<>();

        int mapSize = gameMap.getMapSize();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int nx = center.getX() + dx;
                int ny = center.getY() + dy;

                if (nx >= 0 && ny >= 0 && nx < mapSize && ny < mapSize) {
                    int manhattan = Math.abs(dx) + Math.abs(dy);
                    if (manhattan <= radius) {
                        nearbyNodes.add(new Node(nx, ny));
                    }
                }
            }
        }
        return nearbyNodes;
    }

    private boolean isInLineWith(Node a, Player b) {
        return a.getX() == b.getX() || a.getY() == b.getY();
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

//    private void pickNearestArmor(GameContext ctx) throws IOException {
//        System.out.println("Pick nearest armor.");
//        if (ctx.nearestArmor == null) {
//            System.out.println("No armor found.");
//        }
//        String pathToArmor = PathUtils.getShortestPath(ctx.gameMap, getNodesToAvoid(ctx.gameMap), ctx.player, ctx.nearestArmor, false);
//        if (pathToArmor != null) {
//            if (pathToArmor.isEmpty()) {
//                if (hero.getInventory().getArmor() == null) {
//                    hero.pickupItem();
//                }
//                else {
//                    hero.pickupItem();
//                    hero.revokeItem(hero.getInventory().getArmor().getId());
//                }
//                if (hero.getInventory().getHelmet() == null) {
//                    hero.pickupItem();
//                }
//                else {
//                    hero.pickupItem();
//                    hero.revokeItem(hero.getInventory().getHelmet().getId());
//                }
//
//            } else {
//                hero.move(pathToArmor);
//            }
//        }
//    }
private void pickNearestArmor(GameContext ctx) throws IOException {
    System.out.println("Pick nearest armor.");
    if (ctx.nearestArmor == null) {
        System.out.println("No armor found.");
        return;
    }

    String pathToArmor = PathUtils.getShortestPath(
            ctx.gameMap,
            getNodesToAvoid(ctx.gameMap),
            ctx.player,
            ctx.nearestArmor,
            false
    );

    if (pathToArmor == null) {
        System.out.println("No path to armor.");
        return;
    }

    if (pathToArmor.isEmpty()) {
        // Đang đứng ngay armor, kiểm tra loại armor là ARMOR hay HELMET
        Element element = ctx.gameMap.getElementByIndex(
                ctx.nearestArmor.getX(),
                ctx.nearestArmor.getY()
        );
        if (element == null) {
            System.out.println("No element found at armor location.");
            return;
        }

        switch (element.getType()) {
            case ARMOR:
                if (hero.getInventory().getArmor() != null) {
                    System.out.println("Revoking old armor: " + hero.getInventory().getArmor().getId());
                    hero.revokeItem(hero.getInventory().getArmor().getId());
                }
                hero.pickupItem();
                System.out.println("Picked up new armor.");
                break;

            case HELMET:
                if (hero.getInventory().getHelmet() != null) {
                    System.out.println("Revoking old helmet: " + hero.getInventory().getHelmet().getId());
                    hero.revokeItem(hero.getInventory().getHelmet().getId());
                }
                hero.pickupItem();
                System.out.println("Picked up new helmet.");
                break;

            default:
                System.out.println("Element at target is not armor or helmet: " + element.getType());
                break;
        }

    } else {
        // Chưa đến, di chuyển
        hero.move(pathToArmor);
        System.out.println("Moving to armor with path: " + pathToArmor);
    }
}

    private void pickNearestToSupportItem(GameContext ctx) throws IOException {
        System.out.println("Searching for supportItem");
        String supportItem = PathUtils.getShortestPath(ctx.gameMap, getNodesToAvoid(ctx.gameMap), ctx.player,ctx.nearestSupportItem,false);
        if (supportItem != null) {
            if (supportItem.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(supportItem);
            }
        }
    }

    private void pickNearestSpecial(GameContext ctx) throws IOException {
        Weapon special = ctx.nearestSpecial;
        String pathToGetSpecial = getShortestPath(ctx.gameMap, ctx.nodesToAvoid, ctx.player, special, false);
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

    private void pickNearestThrowable(GameContext ctx) throws IOException {
        Weapon throwable = ctx.nearestThrowable;
        String pathToGetThrowable = getShortestPath(ctx.gameMap, ctx.nodesToAvoid, ctx.player, throwable, false);
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

    private boolean checkItemAround(GameContext ctx) {
        Weapon gun = ctx.nearestGun;
        Weapon melee = ctx.nearestMelee;
        Weapon special = ctx.nearestSpecial;
        Node chest = ctx.nearestChest;
        Armor armor = ctx.nearestArmor;
        SupportItem supportItem = ctx.nearestSupportItem;

        System.out.println("check Item Around");

        if (gun != null) System.out.println("dis gun: " + distance(gun, ctx.player));
        if (melee != null) System.out.println("dis melee: " + distance(melee, ctx.player));
        if (special != null) System.out.println("dis special: " + distance(special, ctx.player));
        if (chest != null) System.out.println("dis chest: " + distance(chest, ctx.player));
        if (armor != null) System.out.println("dis armor: " + distance(armor, ctx.player));
        if (supportItem != null) System.out.println("dis support: " + distance(supportItem, ctx.player));

        return (gun != null && distance(gun, ctx.player) <= 3)
                || (melee != null && distance(melee, ctx.player) <= 3)
                || (special != null && distance(special, ctx.player) <= 3)
                || (chest != null && distance(chest, ctx.player) <= 3)
                || (armor != null && distance(armor, ctx.player) <= 3 &&
                (hero.getInventory().getArmor() == null || hero.getInventory().getHelmet() == null))
                || (supportItem != null && distance(supportItem, ctx.player) <= 3 &&
                hero.getInventory().getListSupportItem().size() <= 3);
    }

    private void getItemAround(GameContext ctx) throws IOException {
        System.out.println("GO around to get Armor and SpItems and enhance Weapon");

        Weapon gun = ctx.nearestGun;
        Weapon melee = ctx.nearestMelee;
        Weapon special = ctx.nearestSpecial;
        Weapon throwable = ctx.nearestThrowable;
        Node chest = ctx.nearestChest;
        Armor armor = ctx.nearestArmor;
        SupportItem supportItem = ctx.nearestSupportItem;
        System.out.println("see some Items around");

        if (gun != null && PathUtils.distance(gun, ctx.player) <= 3) {
            System.out.println("go to get gun:");
            String pathToGun = findPathToGun(ctx.gameMap, getNodesToAvoid(ctx.gameMap), ctx.player);
            if (pathToGun != null) {
                if (pathToGun.isEmpty()) {
                    System.out.println("go pick gun");
                    if (hero.getInventory().getGun() == null) {
                        System.out.println("just pick");
                        hero.pickupItem();
                    } else {
                        System.out.println("thrown weapon to pick a new gun");
                        hero.pickupItem();
                        hero.revokeItem(hero.getInventory().getGun().getId());
                    }
                } else {
                    hero.move(pathToGun);
                }
            }
        }
        if (melee != null && PathUtils.distance(melee,ctx.player) <= 3) {
            System.out.println("Pick melee");
            String pathToGetMelee = findPathToMelee(ctx.gameMap, getNodesToAvoid(ctx.gameMap), ctx.player);
            if (pathToGetMelee != null) {
                if (pathToGetMelee.isEmpty()) {
                    if (hero.getInventory().getMelee().getId().equals("HAND")) {
                        System.out.println("dont have melee and pick");
                        hero.pickupItem();
                    } else {
                        hero.pickupItem();
                        System.out.println("have melee and change to pick");
                        hero.revokeItem(hero.getInventory().getMelee().getId());
                    }
                } else {
                    hero.move(pathToGetMelee);
                }
            }
        }
        if (supportItem != null && PathUtils.distance(supportItem, ctx.player) <= 3 &&
                hero.getInventory().getListSupportItem().size() <= 3) {
            System.out.println("Pick support item");
            pickNearestToSupportItem(ctx);
        }
        if (throwable != null && PathUtils.distance(throwable, ctx.player) <= 3) {
            System.out.println("Pick throwable");
            pickNearestThrowable(ctx);
        }
        if (chest != null && PathUtils.distance(chest, ctx.player) <= 3) {
            System.out.println("pick chest");
            pickNearestChest(ctx);
        }
        if (special != null && PathUtils.distance(special, ctx.player) <= 3) {
            System.out.println("Pick special weapon");
            pickNearestSpecial(ctx);
        }
        if (armor != null){
            if (PathUtils.distance(armor, ctx.player) <= 3) {
                if (hero.getInventory().getArmor() == null || hero.getInventory().getHelmet() == null) {
                    pickNearestArmor(ctx);
                }
                else if (hero.getInventory().getArmor()!= null && armor.getDamageReduce() > hero.getInventory().getArmor().getDamageReduce()) {
                    pickNearestArmor(ctx);
                }

            }
        }
    }

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
        public Weapon nearestThrowable;
        public Player nearestBot;
        public SupportItem nearestSupportItem;
        public Weapon nearestSpecial;

        public List<Bullet> bullets;
        public List<Player> enemies;
        public List<Item> items;

        public List<Node> stuffs = new ArrayList<>();

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
            if (!stuffs.isEmpty()) {
                stuffs.sort(Comparator.comparingInt(p -> distance(p, this.player)));
            }


            // Cache nearest items
            this.nearestGun = getNearestGun(gameMap, player);
            this.nearestMelee = getNearestMelee(gameMap, player);
            this.nearestChest = getNearestChest(gameMap, player);
            this.nearestArmor = getNearestArmor(gameMap, player);
            this.nearestThrowable = getNearestThrowableInRange(gameMap, player);
            this.nearestBot = getNearestBot(gameMap, player);
            this.nearestSupportItem = getNearestSupportItem(gameMap, player);
            this.nearestSpecial = getNearestSpecial(gameMap, player);
        }
    }

    private boolean shouldHeal(GameContext ctx) {
        return ctx.player.getHealth() < 80 && !hero.getInventory().getListSupportItem().isEmpty();
    }
//    private void heal(GameContext ctx) throws IOException {
//        hero.move(getShortestPath(ctx.gameMap,ctx.nodesToAvoid,ctx.player,ctx.nearestSupportItem,false));
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

    private boolean shouldBreakChest(GameContext ctx) {
        if (ctx.nearestChest == null)
            return false;
        return hero.getInventory().getMelee().getId().equals("HAND");
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
        if (hero.getInventory().getThrowable() != null) {
            attackNearestBotWithThrowableAndGun(ctx);
        }
        else{
            attackNearestBot(ctx);
        }
    }






}