import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.npcs.Ally;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Bullet;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static jsclub.codefest.sdk.algorithm.PathUtils.*;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "102995" ;
    private static final String PLAYER_NAME = "Noobslearn2code";
    private static final String SECRET_KEY = "sk-_2Aq7gTHQC6lVatMMbLInA:xRnFCNn0V3hoOZA4Iy4RL66QfAC4RPnMw8BvV7gH32KgVBjbaU1kHh48wxGLPezcxTK7rkDF-LHK4AtDm1TskA";


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
    private int countTimeToRandomMove = 8;
    private int numberOfMoved = 0;
    boolean justOpenedChest = false;
    private int chestAttackRange = 6;
    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
//    public void call(Object... args) {
//        try {
//            if (args == null || args.length == 0) return;
//
//            GameMap gameMap = hero.getGameMap();
//            gameMap.updateOnUpdateMap(args[0]);
//            Player player = gameMap.getCurrentPlayer();
//
//            if (player == null || player.getHealth() == 0) {
//                System.out.println("Player is dead or data is not available.");
//                return;
//            }
//
//            List<Node> nodesToAvoid = getNodesToAvoid(gameMap);
//            if (numberOfMoved >= countTimeToRandomMove) {
//                String randomMove;
//
//                int dir = (int)(Math.random() * 4);
//                switch (dir) {
//                    case 0:
//                        randomMove = "u";
//                        break;
//                    case 1:
//                        randomMove = "d";
//                        break;
//                    case 2:
//                        randomMove = "l";
//                        break;
//                    default:
//                        randomMove = "r";
//                        break;
//                }
//
//                hero.move(randomMove);
//                System.out.println("[BOT] Random move triggered after " + countTimeToRandomMove + " moves: " + randomMove);
//
//                numberOfMoved = 0; // reset sau khi random move
//                return ;
//            }
//
//            if (player.getHealth() <= 10 && findPathToHealth(gameMap,nodesToAvoid,player)!=null) {
//                // Ưu tiên tiếp theo: Hồi phục khi máu thấp
//                heal(gameMap, nodesToAvoid, player);
//                return ;
//            }
//            if (hero.getInventory().getGun() == null && getNearestGun(gameMap,player) != null){
//                handleSearchForGun(gameMap,player,nodesToAvoid);
//                return ;
//            }
//            if(hero.getInventory().getMelee().getId().equals("HAND") && getNearestChest(gameMap,player) != null){
//                pickNearestChest(gameMap,player,nodesToAvoid);
//                return;
//            }
//            if(getNearestArmorInRange(gameMap,player) != null){
//                if (hero.getInventory().getArmor() == null) {
//                    pickNearestArmor(gameMap, player, nodesToAvoid);
//                    return;
//                }
//                if(getNearestArmorInRange(gameMap,player).getDamageReduce() > hero.getInventory().getArmor().getDamageReduce()){
//                    pickNearestArmor(gameMap,player,nodesToAvoid);
//                    return;
//                }
//
//            }
////            if(getNearestSpecialInRange(gameMap,player) != null){
////                pickNearestSpecial(gameMap,player,nodesToAvoid);
////            }
//            if(getNearestThrowableInRange(gameMap,player) != null){
//                if(hero.getInventory().getThrowable() == null){
//                    pickNearestThrowable(gameMap,player,nodesToAvoid);
//                    return;
//                }
//                if(getNearestThrowableInRange(gameMap,player).getDamage() > hero.getInventory().getThrowable().getDamage()){
//                    pickNearestThrowable(gameMap,player,nodesToAvoid);
//                    return ;
//                }
//            }
//            if (hero.getInventory().getMelee().getId().equals("HAND") && getNearestMelee(gameMap,player) != null){
//                pickNearestWeapon(gameMap,player,nodesToAvoid);
//                return;
//            }
//
//            // Cuối cùng: Tấn công khi đã có súng và máu ổn
//            attackNearestBot(gameMap, player, nodesToAvoid);
//
//
//
//
//
//
//
//            // End code here
//            System.out.println("Last shoot time: " + lastShootTime);return ;
//
//
//        } catch (Exception e) {
//            System.err.println("Critical error in call method: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();

            if (player == null || player.getHealth() == 0) {
                System.out.println("Player is dead or data is not available.");
                return;
            }

            List<Node> nodesToAvoid = getNodesToAvoid(gameMap);

            // 1️⃣ Random move an toàn nếu đủ tick
            if (numberOfMoved >= countTimeToRandomMove) {
                String randomMove;

               int dir = (int)(Math.random() * 4);
               switch (dir) {                    case 0:                    randomMove = "u";
                        break;
                    case 1:
                        randomMove = "d";
                        break;
                    case 2:
                        randomMove = "l";
                        break;
                    default:
                        randomMove = "r";
                        break;
                }

                hero.move(randomMove);
                System.out.println("[BOT] Random move triggered after " + countTimeToRandomMove + " moves: " + randomMove);
                numberOfMoved = 0;
                return;
            }

            // 2️⃣ Loot item gần sau khi phá rương
            if (justOpenedChest) {
                if (pickupNearbyLoot(gameMap, player, nodesToAvoid)) {
                    System.out.println("[BOT] Looted nearby item after chest.");
                    return;
                } else {
                    justOpenedChest = false; // không còn loot gần
                }
            }

            // 3️⃣ Cache các giá trị để tránh gọi lặp
            Weapon nearestGun = getNearestGun(gameMap, player);
            Obstacle nearestChest = getNearestChest(gameMap, player);
            Weapon nearestMelee = getNearestMelee(gameMap, player);
            Armor nearestArmor = getNearestArmorInRange(gameMap, player);
            Weapon nearestThrowable = getNearestThrowableInRange(gameMap, player);
            Player nearestBot = getNearestBot(gameMap, player);

            // 4️⃣ FSM ưu tiên:
            // Heal khi máu thấp
            if (player.getHealth() <= 5 && findPathToHealth(gameMap, nodesToAvoid, player) != null) {
                heal(gameMap, nodesToAvoid, player);
                return;
            }

            // Tìm súng khi chưa có
            if (hero.getInventory().getGun() == null && nearestGun != null) {
                handleSearchForGun(gameMap, player, nodesToAvoid);
                return;
            }

            // Nhặt melee trước khi phá rương
            if (hero.getInventory().getMelee().getId().equals("HAND") && nearestMelee != null) {
                pickNearestWeapon(gameMap, player, nodesToAvoid);
                return;
            }

            // Phá rương khi còn cầm tay không
            if (hero.getInventory().getMelee().getId().equals("HAND") && nearestChest != null) {
                pickNearestChest(gameMap, player, nodesToAvoid);
                justOpenedChest = true;
                return;
            }

            // Loot armor nếu tốt hơn hoặc chưa có
            if (nearestArmor != null) {
                if (hero.getInventory().getArmor() == null){
                    pickNearestArmor(gameMap, player, nodesToAvoid);
                    return;
                }
                else {
                    hero.revokeItem(hero.getInventory().getArmor().getId());
                    pickNearestArmor(gameMap, player, nodesToAvoid);
                    return;
                }
            }

            // Loot throwable nếu tốt hơn hoặc chưa có
            if (nearestThrowable != null) {
                if (hero.getInventory().getThrowable() == null) {
                    pickNearestThrowable(gameMap, player, nodesToAvoid);
                    return;
                }
                else {
                    hero.revokeItem(hero.getInventory().getThrowable().getId());
                    pickNearestThrowable(gameMap, player, nodesToAvoid);
                }
            }

            // 5️⃣ Cuối cùng: Attack bot
            if (nearestChest != null && nearestBot != null) {
                int distToChest = distance(player, nearestChest);
                int distToBot = distance(player, nearestBot);

                if (distToChest < distToBot && distToChest <= chestAttackRange) {
                    if(!getShortestPath(gameMap,nodesToAvoid,player,nearestChest,false).isEmpty()){
                        hero.move(getShortestPath(gameMap,nodesToAvoid,player,nearestChest,false));
                    }else{
                        hero.attack(directionToChest(player,nearestChest));
                        System.out.println("[BOT] Attacking chest as it is closer than bot within range " + chestAttackRange);
                        return;
                    }
                }
            }

            attackNearestBot(gameMap, player, nodesToAvoid);

            System.out.println("Last shoot time: " + lastShootTime);

        } catch (Exception e) {
            System.err.println("Critical error in call method: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean pickupNearbyLoot(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        int pickupRange = 1;

        Weapon melee = getNearestWeapon(gameMap, player);
        if (hero.getInventory().getMelee().getId().equals("HAND") && melee != null) {
            pickNearestWeapon(gameMap, player, nodesToAvoid);
            return true;
        }

        Weapon gun = getNearestGun(gameMap, player);
        if (hero.getInventory().getGun() == null && gun != null) {
            handleSearchForGun(gameMap, player, nodesToAvoid);
            return true;
        }

        Weapon throwable = getNearestThrowableInRange(gameMap, player);
        if (hero.getInventory().getThrowable() == null && throwable != null) {
            pickNearestThrowable(gameMap, player, nodesToAvoid);
            return true;
        }

        Armor armor = getNearestArmorInRange(gameMap, player);
        if (hero.getInventory().getArmor() == null && armor != null) {
            pickNearestArmor(gameMap, player, nodesToAvoid);
            return true;
        }

        return false;
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
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestGun, false);
    }


    // Private Function

    // Find object
    private Weapon getNearestGun(GameMap gameMap, Player player) {
        List<Weapon> guns = gameMap.getAllGun();
        Weapon nearestGun = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon gun : guns) {
            if (gun.getX() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                gun.getX() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()) ||
                gun.getY() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    gun.getY() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()))
            {
                continue;
            }
            double distance = distance(player, gun);
            if (distance < minDistance) {
                minDistance = distance;
                nearestGun = gun;
            }
        }
        return nearestGun;
    }

    private Player getNearestBot(GameMap gameMap, Player player) {
        List<Player> enemies = gameMap.getOtherPlayerInfo();
        Player nearestBot = null;
        double minDistance = Double.MAX_VALUE;
        for (Player e : enemies) {
            if (e.getX() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    e.getX() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()) ||
                    e.getY() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    e.getY() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()))
            {
                continue;
            }
            double distance = distance(player, e);
            if (distance < minDistance) {
                minDistance = distance;
                nearestBot = e;
            }
        }
        return nearestBot;
    }

    private Obstacle getNearestChest(GameMap gameMap, Player player) {
        List<Obstacle> chests = gameMap.getObstaclesByTag("DESTRUCTIBLE");
        Obstacle nearestChest = null;
        double minDistance = Double.MAX_VALUE;
        for (Obstacle chest : chests) {
            if (chest.getX() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    chest.getX() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()) ||
                    chest.getY() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    chest.getY() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()))
            {
                continue;
            }
            double distance = distance(player, chest);
            if (distance < minDistance) {
                minDistance = distance;
                nearestChest = chest;
            }
        }
        return nearestChest;
    }

    private Ally getNearestAlliance(GameMap gameMap,Player player) {
        List<Ally> allys = gameMap.getListAllies();
        Ally nearestAlliance = null;
        double minDistance = Double.MAX_VALUE;
        for (Ally ally : allys) {
            if (ally.getX() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    ally.getX() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()) ||
                    ally.getY() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    ally.getY() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()))
            {
                continue;
            }
            double distance = distance(player, ally);
            if (distance < minDistance) {
                minDistance = distance;
                nearestAlliance = ally;
            }
        }
        return nearestAlliance;
    }
    private Armor getNearestArmorInRange(GameMap gameMap,Player player) {
        List<Armor> armors = gameMap.getListArmors();
        Armor nearestArmor = null;
        double minDistance = Double.MAX_VALUE;
        for (Armor armor : armors) {
            if(!checkInsideSafeArea(armor, gameMap.getSafeZone(), gameMap.getMapSize())){
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

    private SupportItem getNearestSupportItemInRange(GameMap gameMap,Player player) {
        List<SupportItem> supportItems = gameMap.getListSupportItems();
        SupportItem nearestSupportItem = null;
        double minDistance = Double.MAX_VALUE;
        for (SupportItem supportItem : supportItems) {
            if (checkInsideSafeArea(supportItem, gameMap.getSafeZone(), gameMap.getMapSize())){
                double distance = distance(player, supportItem);
                if (distance < minDistance && distance < 4) {
                    minDistance = distance;
                    nearestSupportItem = supportItem;
                }
            }
        }
        return  nearestSupportItem;
    }
// Get nearest melee
    private Weapon getNearestMelee(GameMap gameMap, Player player) {
        List<Weapon> Melees = gameMap.getAllMelee();
        Weapon nearestMelee = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon melee : Melees) {
            if (checkInsideSafeArea(melee, gameMap.getSafeZone(), gameMap.getMapSize())) {
                double distance = distance(player, melee);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestMelee = melee;
                }
            }
        }
        return nearestMelee;
    }
    // Get Nearest Throwable Weapon
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

    private Weapon getNearestSpecialInRange(GameMap gameMap, Player player) {
        List<Weapon> specials = gameMap.getAllSpecial();
        Weapon nearestSpecial = null;
        double minDistance = Double.MAX_VALUE;
        for (Weapon special : specials) {
            if (checkInsideSafeArea(special, gameMap.getSafeZone(), gameMap.getMapSize())) {
                double distance = distance(player, special);
                if (distance < minDistance && distance < 4) {
                    minDistance = distance;
                    nearestSpecial = special;
                }
            }
        }
        return nearestSpecial;
    }


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

    private Weapon checkDistanceAndUpdate(Player player, Weapon newWeapon, Weapon currentWeapon, double currentMinDistance) {
        if (newWeapon != null) {
            double newDistance = distance(player, newWeapon);
            if (currentWeapon == null || newDistance < currentMinDistance) {
                return newWeapon;
            }
        }
        return currentWeapon;
    }



    // Find a path
    private String findPathToEnemy(GameMap gameMap,List<Node> nodesToAvoid, Player player) {
        Player e =  getNearestBot(gameMap, player);
        if (e == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, e, false);
    }

    private String findPathToChest(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Obstacle nearestChest = getNearestChest(gameMap, player);
        if (nearestChest == null) {
            System.out.println("No nearest chest found.");
            return null;
        }
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestChest, false);
    }
    private String findPathToHealth(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Ally nearestHealth = getNearestAlliance(gameMap, player);
        if (nearestHealth == null) {
            System.out.println("No nearest health found.");
            return null;
        }
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestHealth, false);
    }
    private String findPathToArmor(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Armor nearestArmor = getNearestArmorInRange(gameMap,player);
        if (nearestArmor == null) {
            System.out.println("No nearest armor found.");
            return null;
        }
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestArmor, false);
    }
    private String findPathToWeapon(GameMap gameMap, List<Node> nodesToAvooid, Player player){
        Weapon nearestWeapon = getNearestWeapon(gameMap, player);
        if(nearestWeapon == null){
            System.out.println("No nearest weapon found");
            return null;
        }
        return PathUtils.getShortestPath(gameMap,nodesToAvooid,player,nearestWeapon,false);
    }

    private String findPathToSpecial(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Weapon nearestSpecial = getNearestSpecialInRange(gameMap, player);
        if (nearestSpecial == null) {
            System.out.println("No nearest special found.");
            return null;
        }
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestSpecial, false);
    }

    private String findPathToThrowable(GameMap gameMap, List<Node> nodesToAvoid, Player player){
        Weapon nearestThrowable = getNearestThrowableInRange(gameMap, player);
        if (nearestThrowable == null) {
            System.out.println("No nearest throwable found.");
            return null;
        }
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestThrowable, false);
    }


    private boolean isDangerNearby(GameMap gameMap, Player player) {
        List<Node> dangerNodes = preCaculatedNode(gameMap);
        if (dangerNodes.isEmpty()) return false;
        for (Node danger : dangerNodes) {
            if (danger.getX() == player.getX() && danger.getY() == player.getY()) {
                return true;
            }
        }
        return false;
    }



    // Moving and doing
//    private void attackNearestBot(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
//        Player e = getNearestBot(gameMap, player);
//        if (e == null) return;
//
//        // Check đạn sắp tới trước, nếu có => ưu tiên di chuyển tránh
//        if (isDangerNearby(gameMap, player)) {
//            System.out.println("[Bot] Danger detected! Prioritizing dodge.");
//            List<Node> tmpNodesToAvoid = preCaculatedNode(gameMap);
//            nodesToAvoid.addAll(tmpNodesToAvoid);
//            String safePath = findPathToEnemy(gameMap, nodesToAvoid, player); // tìm path an toàn
//            hero.move(safePath);
//            nodesToAvoid.removeAll(tmpNodesToAvoid);
//            return;
//        }
//
//        String pathToBot = findPathToEnemy(gameMap, nodesToAvoid, player);
//        if (pathToBot == null) return;
//
//        int distanceX = Math.abs(player.getX() - e.getX());
//        int distanceY = Math.abs(player.getY() - e.getY());
//        int gunRange = hero.getInventory().getGun().getRange()[1];
//        int currentStep = gameMap.getStepNumber();
//        double cooldown = hero.getInventory().getGun().getCooldown();
//        boolean canShoot = (currentStep - lastShootTime >= Math.ceil(cooldown));
//
//        // Bắn nếu đủ cooldown và trong tầm
//        if (player.getX() == e.getX() && distanceY <= gunRange && canShoot) {
//            hero.shoot(player.getY() > e.getY() ? "d" : "u");
//            lastShootTime = currentStep;
//            System.out.println("[Bot] Shoot vertically.");
//            return;
//        }
//
//        if (player.getY() == e.getY() && distanceX <= gunRange && canShoot) {
//            hero.shoot(player.getX() < e.getX() ? "r" : "l");
//            lastShootTime = currentStep;
//            System.out.println("[Bot] Shoot horizontally.");
//            return;
//        }
//
//        // Nếu đã sát enemy => melee
//        if (distanceX + distanceY == 1) {
//            String dir = directionTo(player, e);
//            hero.attack(dir);
//            System.out.println("[Bot] Melee attack: " + dir);
//            return;
//        }
//
//        // Nếu không có đạn nguy hiểm => tiếp tục di chuyển đến bot
//        List<Node> tmpNodesToAvoid = preCaculatedNode(gameMap);
//        nodesToAvoid.addAll(tmpNodesToAvoid);
//        pathToBot = findPathToEnemy(gameMap, tmpNodesToAvoid, player);
//        if (pathToBot != null) {
//            hero.move(pathToBot);
//            System.out.println("[Bot] Move to bot: " + pathToBot);
//        }
//        nodesToAvoid.removeAll(tmpNodesToAvoid);
//    }
    private void attackNearestBot(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        Player e = getNearestBot(gameMap, player);
        if (e == null) return;

        String pathToBot = findPathToEnemy(gameMap, nodesToAvoid, player);
        if (pathToBot == null) return;

        int distanceX = Math.abs(player.getX() - e.getX());
        int distanceY = Math.abs(player.getY() - e.getY());
        int gunRange = hero.getInventory().getGun().getRange()[1];
        int currentStep = gameMap.getStepNumber();
        double cooldown = hero.getInventory().getGun().getCooldown();
        boolean canShoot = (currentStep - lastShootTime >= cooldown);

        // Use throwable if an enemy in range
        if (hero.getInventory().getThrowable() != null) {
            int throwRange = hero.getInventory().getThrowable().getRange()[1];
            int explosionRange = hero.getInventory().getThrowable().getExplodeRange();
            String[] directions = {"u", "d", "l", "r"};

            for (String dir : directions) {
                for (int d = 1; d <= throwRange; d++) {
                    if (willHitEnemyWithThrowable(player, e, dir, d, explosionRange)) {
                        hero.throwItem(dir);
                        System.out.println("[Bot] Threw throwable at enemy in direction: " + dir);
                        return;
                    }
                }
            }
        }


        // Bắn nếu đủ cooldown và trong tầm
        if (player.getX() == e.getX() && distanceY <= gunRange && canShoot) {
            hero.shoot(player.getY() > e.getY() ? "d" : "u");
            lastShootTime = currentStep;
            return;
        }

        if (player.getY() == e.getY() && distanceX <= gunRange && canShoot) {
            hero.shoot(player.getX() < e.getX() ? "r" : "l");
            lastShootTime = currentStep;
            return;
        }

        // Nếu pathToBot rỗng (đã sát enemy) => melee
        if (pathToBot.length()==1) {
            String dir = directionTo(player, e);
            hero.attack(dir);
            System.out.println("Attack " + dir);
            return;
        }

        // Nếu không bắn, không attack => di chuyển
        hero.move(pathToBot);
        numberOfMoved++;

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
    
        // If not in same row/column, return empty string
        return "";
    }
    private String directionToChest(Player player, Obstacle e) {
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

        // If not in same row/column, return empty string
        return "";
    }

    private boolean willHitEnemyWithThrowable(Player player, Player enemy, String direction, int throwDistance, int explosionRange) {
        int explosionX = player.getX();
        int explosionY = player.getY();

        switch (direction) {
            case "d":
                explosionY -= throwDistance;
                break;
            case "u":
                explosionY += throwDistance;
                break;
            case "l":
                explosionX -= throwDistance;
                break;
            case "r":
                explosionX += throwDistance;
                break;
            default:
                return false; // Phòng lỗi nếu truyền sai direction
        }

        int distance = Math.abs(explosionX - enemy.getX()) + Math.abs(explosionY - enemy.getY());
        return distance <= explosionRange;
    }



    private void pickNearestChest(GameMap gameMap,Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("Getting nearest chest.");
        String pathToChest = findPathToChest(gameMap, nodesToAvoid, player);
        if (pathToChest == null) {
            System.out.println("No nearest chest found.");
            return;
        }
        System.out.println("Chest found: " + pathToChest);
        if (pathToChest.equals("u") ||  pathToChest.equals("r") || pathToChest.equals("l") ||  pathToChest.equals("d")) {
            hero.attack(pathToChest);
        }
        else {
            hero.move(pathToChest);
            numberOfMoved ++;
        }
    }

    private void heal(GameMap gameMap,List<Node> nodesToAvoid, Player player) throws IOException {
        System.out.println("Getting nearest health.");
        String pathToHealth = findPathToHealth(gameMap, nodesToAvoid, player);
        if (pathToHealth == null) {
            System.out.println("No nearest health found.");
            return;
        }
        System.out.println("Health found: " + pathToHealth);
        hero.move(pathToHealth);
        numberOfMoved ++;

    }
    private void handleSearchForGun(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No gun found. Searching for a gun.");
        String pathToGun = findPathToGun(gameMap, nodesToAvoid, player);

        if (pathToGun != null) {
            if (pathToGun.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(pathToGun);
                numberOfMoved ++;
            }
        }
    }

    private void pickNearestArmor(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No armor found. Searching for a armor.");
        String pathToArmor = findPathToArmor(gameMap, nodesToAvoid, player);
        if (pathToArmor != null) {
            if (pathToArmor.isEmpty()) {
                hero.pickupItem();
            }
            else {
                hero.move(pathToArmor);
                numberOfMoved ++;
            }
        }
    }
    private void pickNearestSpecial(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No special found. Searching for a special.");
        String pathToSpecial = findPathToSpecial(gameMap, nodesToAvoid, player);
        if (pathToSpecial != null) {
            if (pathToSpecial.isEmpty()) {
                hero.pickupItem();
            }
            else {
                hero.move(pathToSpecial);
                numberOfMoved ++;
            }
        }
    }
    private void pickNearestThrowable(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No throwable found. Searching for a throwable.");
        String pathToThrowable = findPathToThrowable(gameMap, nodesToAvoid, player);
        if (pathToThrowable != null) {
            if (pathToThrowable.isEmpty()) {
                hero.pickupItem();
            }
            else {
                hero.move(pathToThrowable);
                numberOfMoved ++;
            }
        }
    }
    private void pickNearestWeapon(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws  IOException{
        System.out.println("Searching until full weapon");
        String pathToWeapon = findPathToWeapon(gameMap,nodesToAvoid,player);
        if (pathToWeapon != null){
            if (pathToWeapon.isEmpty()){
                hero.pickupItem();
            }
            else {
                hero.move(pathToWeapon);
                numberOfMoved ++;
            }
        }
    }
    private List<Node> preCaculatedNode(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>();
        if (gameMap.getListBullets() == null) return nodes;

        for (int i = 0; i < gameMap.getListBullets().size(); i++) {
            Bullet bullet = gameMap.getListBullets().get(i);
            Node position = bullet.getPosition();
            if (position == null) continue;

            int dx = Integer.compare(bullet.getDestinationX(), position.getX());
            int dy = Integer.compare(bullet.getDestinationY(), position.getY());

            for (int j = 1; j <= bullet.getSpeed(); j++) {
                int newX = position.getX() + (dx * j) + 1;
                int newY = position.getY() + (dy * j) + 1;

                // Add boundary check based on map size
                if (isValidPosition(newX, newY, gameMap)) {
                    nodes.add(new Node(newX, newY));
                }
            }
        }
        return nodes;
    }

    private boolean isValidPosition(int x, int y, GameMap gameMap) {
        return x >= 0 && x < gameMap.getMapSize() &&
           y >= 0 && y < gameMap.getMapSize();
}


}