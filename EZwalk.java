package org.dreambot.scriptmain;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.script.ScriptManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

@ScriptManifest(name = "EZwalk - Auto Travel", description = "Comprehensive automated travel system for RuneScape locations with energy management and random exploration feature", author = "Loud", version = 1.1, category = Category.UTILITY)
public class EZwalk extends AbstractScript {

    // Travel locations enum
    private enum TravelLocation {
        GRAND_EXCHANGE(new Tile(3165, 3465, 0), "Grand Exchange"),
        LUMBRIDGE(new Tile(3222, 3218, 0), "Lumbridge"),
        VARROCK_CENTER(new Tile(3212, 3423, 0), "Varrock Center"),
        FALADOR_CENTER(new Tile(2965, 3379, 0), "Falador Center"),
        AL_KHARID(new Tile(3293, 3182, 0), "Al Kharid"),
        DRAYNOR_VILLAGE(new Tile(3093, 3243, 0), "Draynor Village"),
        DRAYNOR_MANOR(new Tile(3109, 3330, 0), "Draynor Manor"),
        AL_KHARID_MINE(new Tile(3299, 3271, 0), "Al Kharid Mine"),
        PORT_SARIM(new Tile(3042, 3243, 0), "Port Sarim"),
        BANDIT_CAMP(new Tile(3039, 3696, 0), "Bandit Camp (Wilderness)"),
        BLACK_KNIGHTS_FORTRESS(new Tile(3015, 3512, 0), "Black Knights' Fortress"),
        EXPLORE_RANDOM(new Tile(3252, 3250, 0), "Explore Random"),
        VARROCK_EAST_MINE(new Tile(3289, 3371, 0), "Varrock East Mine"),
        LUMBRIDGE_SWAMP(new Tile(3169, 3173, 0), "Lumbridge Swamp"),
        DWARVEN_MINE(new Tile(3058, 9764, 0), "Dwarven Mine"),
        KARAMJA(new Tile(2924, 3177, 0), "Karamja (Musa Point)"),
        MUSA_POINT_DOCK(new Tile(2908, 3151, 0), "Musa Point Dock"),
        LUMBRIDGE_SWAMP_SOUTH(new Tile(3170, 3166, 0), "Lumbridge Swamp South"),
        SECURITY_STRONGHOLD(new Tile(3081, 3421, 1), "Security Stronghold"),
        LUMBRIDGE_COWS(new Tile(3261, 3269, 0), "Lumbridge Cow Field"),
        RIMMINGTON(new Tile(2963, 3220, 0), "Rimmington"),
        BRIMHAVEN(new Tile(2798, 3173, 0), "Brimhaven"),
        WIZARD_TOWER(new Tile(3111, 3166, 0), "Wizard Tower"),
        BARBARIAN_VILLAGE(new Tile(3081, 3421, 0), "Barbarian Village"),
        EDGEVILLE(new Tile(3093, 3493, 0), "Edgeville"),
        SEERS_VILLAGE(new Tile(2722, 3484, 0), "Seers Village"),
        MINING_GUILD(new Tile(3046, 9756, 0), "Mining Guild"),
        CRAFTING_GUILD(new Tile(2933, 3293, 0), "Crafting Guild");

        private final Tile destination;
        private final String name;

        TravelLocation(Tile destination, String name) {
            this.destination = destination;
            this.name = name;
        }

        public Tile getDestination() {
            return destination;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Energy potions that can be used
    private static final String[] ENERGY_POTIONS = {
            "Energy potion(4)", "Energy potion(3)", "Energy potion(2)", "Energy potion(1)",
            "Super energy(4)", "Super energy(3)", "Super energy(2)", "Super energy(1)",
            "Stamina potion(4)", "Stamina potion(3)", "Stamina potion(2)", "Stamina potion(1)"
    };

    // GUI components
    private JFrame frame;
    private JComboBox<TravelLocation> locationComboBox;
    private JButton startWalkingButton;
    private JButton stopWalkingButton;
    private JLabel statusLabel;
    private JCheckBox useEnergyPotionsCheckbox;
    private JSlider energyThresholdSlider;
    private JLabel energyThresholdLabel;

    // Script state variables
    private boolean isWalking = false;
    private TravelLocation selectedLocation = null;
    private boolean useEnergyPotions = true;
    private int energyThreshold = 40;
    private long lastRunEnergyLog = 0;

    // Quick Start variables
    private boolean quickStartEnabled = false;
    private String quickStartDestination = "";
    private int quickStartEnergyThreshold = 40;
    private boolean quickStartUseEnergyPotions = true;

    @Override
    public void onStart() {
        Logger.log("Auto Travel Script started!");

        // Check for quick start parameters
        Map<String, String> parameters = getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            handleQuickStart(parameters);
        } else {
            createGUI();
        }
    }

    @Override
    public int onLoop() {
        if (isWalking && selectedLocation != null) {
            handleWalking();
            return Calculations.random(600, 1200);
        }

        // Idle state - just manage run energy
        checkAndManageRunEnergy();

        return Calculations.random(1000, 2000);
    }

    private void createGUI() {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Auto Travel Script");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLayout(new BorderLayout());
            frame.setResizable(true);

            // Main panel
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Status label
            statusLabel = new JLabel("Status: Ready");
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
            mainPanel.add(statusLabel);
            mainPanel.add(Box.createVerticalStrut(10));

            // Location selection panel
            JPanel locationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            locationPanel.add(new JLabel("Select Destination:"));
            locationComboBox = new JComboBox<>(TravelLocation.values());
            locationComboBox.setPreferredSize(new Dimension(200, 25));
            locationPanel.add(locationComboBox);
            mainPanel.add(locationPanel);
            mainPanel.add(Box.createVerticalStrut(10));

            // Energy settings panel
            JPanel energyPanel = new JPanel();
            energyPanel.setLayout(new BoxLayout(energyPanel, BoxLayout.Y_AXIS));
            energyPanel.setBorder(BorderFactory.createTitledBorder("Energy Settings"));

            // Use energy potions checkbox
            useEnergyPotionsCheckbox = new JCheckBox("Use Energy Potions", useEnergyPotions);
            useEnergyPotionsCheckbox.addActionListener(e -> useEnergyPotions = useEnergyPotionsCheckbox.isSelected());
            useEnergyPotionsCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
            energyPanel.add(useEnergyPotionsCheckbox);

            // Energy threshold slider
            JPanel thresholdPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            energyThresholdLabel = new JLabel("Energy Threshold: " + energyThreshold + "%");
            thresholdPanel.add(energyThresholdLabel);
            energyPanel.add(thresholdPanel);

            energyThresholdSlider = new JSlider(JSlider.HORIZONTAL, 10, 80, energyThreshold);
            energyThresholdSlider.setMajorTickSpacing(10);
            energyThresholdSlider.setMinorTickSpacing(5);
            energyThresholdSlider.setPaintTicks(true);
            energyThresholdSlider.setPaintLabels(true);
            energyThresholdSlider.addChangeListener(e -> {
                energyThreshold = energyThresholdSlider.getValue();
                energyThresholdLabel.setText("Energy Threshold: " + energyThreshold + "%");
            });
            energyPanel.add(energyThresholdSlider);

            mainPanel.add(energyPanel);
            mainPanel.add(Box.createVerticalStrut(15));

            // Control buttons panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

            startWalkingButton = new JButton("Start Walking");
            startWalkingButton.setPreferredSize(new Dimension(120, 30));
            startWalkingButton.addActionListener(e -> {
                selectedLocation = (TravelLocation) locationComboBox.getSelectedItem();
                isWalking = true;
                statusLabel.setText("Status: Walking to " + selectedLocation.toString());
                Logger.log("Starting walk to " + selectedLocation.toString());
            });
            buttonPanel.add(startWalkingButton);

            stopWalkingButton = new JButton("Stop Walking");
            stopWalkingButton.setPreferredSize(new Dimension(120, 30));
            stopWalkingButton.addActionListener(e -> {
                isWalking = false;
                statusLabel.setText("Status: Stopped");
                Logger.log("Stopping travel");
            });
            buttonPanel.add(stopWalkingButton);

            mainPanel.add(buttonPanel);

            // Info panel
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBorder(BorderFactory.createTitledBorder("Information"));

            JLabel infoLabel1 = new JLabel(" Select a destination and click 'Start Walking'");
            JLabel infoLabel2 = new JLabel(" Script will automatically manage run energy");
            JLabel infoLabel3 = new JLabel(" Energy potions will be used if available");

            infoLabel1.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoLabel2.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoLabel3.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(infoLabel1);
            infoPanel.add(infoLabel2);
            infoPanel.add(infoLabel3);

            mainPanel.add(Box.createVerticalStrut(10));
            mainPanel.add(infoPanel);

            frame.add(mainPanel, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Handles walking to the selected location
     */
    private void handleWalking() {
        if (selectedLocation == null) {
            isWalking = false;
            statusLabel.setText("Status: No destination selected");
            return;
        }

        Tile destination = selectedLocation.getDestination();

        // Special handling for Explore Random - generate random coordinates within area
        if (selectedLocation == TravelLocation.EXPLORE_RANDOM) {
            // Area bounds: (2983, 3501, 3521, 2999)
            int randomX = Calculations.random(2983, 3521);
            int randomY = Calculations.random(2999, 3501);
            destination = new Tile(randomX, randomY, 0);
            Logger.log("Generated random exploration point: (" + randomX + ", " + randomY + ")");
        }

        if (destination == null) {
            isWalking = false;
            statusLabel.setText("Status: Invalid destination");
            return;
        }

        // Check if we're already at the destination
        if (Players.getLocal().distance(destination) < 5) {
            isWalking = false;
            if (selectedLocation == TravelLocation.EXPLORE_RANDOM) {
                statusLabel.setText("Status: Exploration point reached! Select again for new location");
                Logger.log("Reached exploration point! Select 'Explore Random' again for a new location.");
            } else {
                statusLabel.setText("Status: Arrived at " + selectedLocation.toString());
                Logger.log("Arrived at " + selectedLocation.toString());
            }
            return;
        }

        // Check and manage run energy before walking
        checkAndManageRunEnergy();

        // Walk to destination
        if (!Walking.walk(destination)) {
            Logger.log("Failed to walk to " + selectedLocation.toString() + ", retrying...");
            Sleep.sleep(1000, 2000);
            return;
        }

        // Update status with distance
        int distance = (int) Players.getLocal().distance(destination);
        if (selectedLocation == TravelLocation.EXPLORE_RANDOM) {
            statusLabel.setText("Status: Exploring random area (Distance: " + distance + ")");
        } else {
            statusLabel.setText("Status: Walking to " + selectedLocation.toString() + " (Distance: " + distance + ")");
        }

        // Sometimes adjust camera while walking for more human-like behavior
        if (Calculations.random(1, 100) < 15) {
            Camera.rotateTo(Calculations.random(0, 360), Calculations.random(30, 70));
        }

        // Add a small delay
        Sleep.sleep(Calculations.random(800, 1500));
    }

    /**
     * Checks and manages run energy - enables running when possible and drinks
     * energy potions when needed
     */
    private void checkAndManageRunEnergy() {
        // Rate limit run energy logs to avoid spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRunEnergyLog < 10000) {
            return;
        }

        // Update timestamp
        lastRunEnergyLog = currentTime;

        // Get current run energy
        int runEnergy = Walking.getRunEnergy();

        // Enable running if energy is above 20%
        if (runEnergy > 20 && !Walking.isRunEnabled()) {
            Walking.toggleRun();
            Logger.log("Enabled running (Energy: " + runEnergy + "%)");
        } // Check if we need to use an energy potion
        if (useEnergyPotions && runEnergy < energyThreshold) {
            if (drinkEnergyPotion()) {
                Logger.log("Drank energy potion (Energy was: " + runEnergy + "%)");
            } else {
                Logger.log("No energy potions found in inventory");
            }
        }
    }

    /**
     * Drinks an energy potion if available
     */
    private boolean drinkEnergyPotion() {
        // Find any energy potion in inventory
        Item energyPotion = Inventory.get(item -> {
            if (item == null || item.getName() == null)
                return false;

            for (String potionName : ENERGY_POTIONS) {
                if (item.getName().equals(potionName)) {
                    return true;
                }
            }
            return false;
        });

        if (energyPotion != null) {
            if (energyPotion.interact("Drink")) {
                Sleep.sleep(1200, 2000);

                // Drop empty vial after drinking
                dropEmptyVials();

                return true;
            }
        }
        return false;
    }

    /**
     * Drops empty vials from inventory to keep it clean
     */
    private void dropEmptyVials() {
        Item vial = Inventory.get("Vial");
        if (vial != null) {
            if (vial.interact("Drop")) {
                Logger.log("Dropped empty vial");
                Sleep.sleep(600, 1000);
            }
        }
    }

    /**
     * Handle quick start with parameters
     */
    private void handleQuickStart(Map<String, String> parameters) {
        try {
            if (parameters.containsKey("destination")) {
                quickStartDestination = parameters.get("destination");
                quickStartEnabled = true;

                // Find matching location
                for (TravelLocation location : TravelLocation.values()) {
                    if (location.name().equalsIgnoreCase(quickStartDestination) ||
                            location.toString().equalsIgnoreCase(quickStartDestination)) {
                        selectedLocation = location;
                        break;
                    }
                }

                if (selectedLocation != null) {
                    // Optional parameters
                    if (parameters.containsKey("energyThreshold")) {
                        quickStartEnergyThreshold = Integer.parseInt(parameters.get("energyThreshold"));
                        energyThreshold = quickStartEnergyThreshold;
                    }

                    if (parameters.containsKey("useEnergyPotions")) {
                        quickStartUseEnergyPotions = Boolean.parseBoolean(parameters.get("useEnergyPotions"));
                        useEnergyPotions = quickStartUseEnergyPotions;
                    }

                    // Start walking immediately
                    isWalking = true;
                    Logger.log("Quick Start: Walking to " + selectedLocation.toString());
                    Logger.log("Quick Start: Energy threshold: " + energyThreshold + "%");
                    Logger.log("Quick Start: Use energy potions: " + useEnergyPotions);
                } else {
                    Logger.log("Quick Start: Invalid destination '" + quickStartDestination
                            + "'. Available destinations:");
                    for (TravelLocation location : TravelLocation.values()) {
                        Logger.log("  - " + location.name() + " (" + location.toString() + ")");
                    }
                    quickStartEnabled = false;
                    createGUI();
                }
            } else {
                Logger.log("Quick Start: No destination specified. Opening GUI...");
                createGUI();
            }
        } catch (Exception e) {
            Logger.log("Quick Start Error: " + e.getMessage());
            Logger.log("Opening GUI instead...");
            createGUI();
        }
    }

    @Override
    public void onExit() {
        if (frame != null) {
            frame.dispose();
        }
        Logger.log("Auto Travel Script stopped!");
    }
}