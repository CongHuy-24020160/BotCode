
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
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static jsclub.codefest.sdk.algorithm.PathUtils.*;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "110624";
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

            if (player == null || player.getHealth() == 0) {
                System.out.println("Player is dead or data is not available.");
                return;
            }

            List<Node> nodesToAvoid = getNodesToAvoid(gameMap);
            if (player.getHealth() <= 10 && findPathToHealth(gameMap,nodesToAvoid,player)!=null) {
                // Ưu tiên tiếp theo: Hồi phục khi máu thấp
                heal(gameMap, nodesToAvoid, player);
            }
            else if (hero.getInventory().getGun() == null && getNearestGun(gameMap,player) != null){
                handleSearchForGun(gameMap,player,nodesToAvoid);
            }
            else if (hero.getInventory().getMelee().getId().equals("HAND") && getNearestMelee(gameMap,player) != null){
                pickNearestWeapon(gameMap,player,nodesToAvoid);
            }
            else if(hero.getInventory().getMelee().getId().equals("HAND") && getNearestChest(gameMap,player) != null){
                pickNearestChest(gameMap,player,nodesToAvoid);
            }
            else {
                // Cuối cùng: Tấn công khi đã có súng và máu ổn
                attackNearestBot(gameMap, player, nodesToAvoid);
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
        nodes.addAll(gameMap.getListEnemies());
        return nodes;
    }

    private String findPathToGun(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Weapon nearestGun = getNearestGun(gameMap, player);
        if (nearestGun == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestGun, false);
    }


    // Private Function

    // Find nearest object
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
    private Armor getNearestArmor(GameMap gameMap,Player player) {
        List<Armor> armors = gameMap.getListArmors();
        Armor nearestArmor = null;
        double minDistance = Double.MAX_VALUE;
        for (Armor armor : armors) {
            if (armor.getX() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    armor.getX() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()) ||
                    armor.getY() < (gameMap.getMapSize()/2 - gameMap.getSafeZone()) ||
                    armor.getY() > (gameMap.getMapSize()/2 + gameMap.getSafeZone()))
            {
                continue;
            }
            double distance = distance(player, armor);
            if (distance < minDistance) {
                minDistance = distance;
                nearestArmor = armor;
            }
        }
        return nearestArmor;
    }

    private SupportItem getNearestSupportItem(GameMap gameMap,Player player) {
        List<SupportItem> supportItems = gameMap.getListSupportItems();
        SupportItem nearestSupportItem = null;
        double minDistance = Double.MAX_VALUE;
        for (SupportItem supportItem : supportItems) {
            if (checkInsideSafeArea(supportItem, gameMap.getSafeZone(), gameMap.getMapSize())){
                double distance = distance(player, supportItem);
                if (distance < minDistance) {
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
    // Get Nearest Throwable  Weapon
    private Weapon getNearestThrowable(GameMap gameMap, Player player) {
        List<Weapon> throwables = gameMap.getAllThrowable();
        Weapon nearestThrowable = null;
        double minDistance = Double.MAX_VALUE;

        for (Weapon throwable : throwables) {
            if (checkInsideSafeArea(throwable, gameMap.getSafeZone(), gameMap.getMapSize())) {
                double distance = distance(player, throwable);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestThrowable = throwable;
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
                double distance = distance(player, special);
                if (distance < minDistance) {
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



    // Find Path to the object
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
        Armor nearestArmor = getNearestArmor(gameMap,player);
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





    // Take Nearest object
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
        } else if (dy == 0) {
            if (dx < 0) {
                return "r";
            } else if (dx > 0) {
                return "l";
            }
        }

        // Nếu không cùng hàng/cột, không trả hướng tấn công
        return "";
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
    }
    private void handleSearchForGun(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No gun found. Searching for a gun.");
        String pathToGun = findPathToGun(gameMap, nodesToAvoid, player);

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
            }
            else {
                hero.move(pathToArmor);
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
            }
        }
    }


}
