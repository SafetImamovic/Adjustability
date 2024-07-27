# Vanilla Data

This table represents every vanilla enchantment with a unique identifier. The data is gathered when the `Adjustability()` constructor is called.

The way the enchantments are gathered is by accessing `ForgeRegistries.ENCHANTMENTS`.

This way any class which extends from the `Enchantment` class will be gathered, this means
that any modded enchantment will be gathered as well.

Generally the way Minecraft handles data in general is not by assigning a unique identifier to each object as an integer,
but by using the `ResourceLocation` class which is a combination of the mod's ID and the name of the enchantment.

This approach avoids potential conflicts between mods that might occur if they tried to use the same integer ID for different enchantments.

<procedure title="File tree structure">
<code-block>
<![CDATA[
.
├── config/
│   ├── adjustability/
│   │   ├── enchantment_data.json
│   │   ├── mutually_exclusive_data.json﻿
│   │   └── ...
│   └── ...
└── ...
]]>
</code-block>
</procedure>


<procedure collapsible="true" title="enchantment_data.json">

<p>This data is populated when the <code>Adjustability()</code> constructor is called</p>
<p>Note that these are all the Enchantments present in Minecraft 1.20.6 without additional mods</p>
<code-block lang="json">
<![CDATA[
[
"minecraft:protection",
"minecraft:fire_protection",
"minecraft:feather_falling",
"minecraft:blast_protection",
"minecraft:projectile_protection",
"minecraft:respiration",
"minecraft:aqua_affinity",
"minecraft:thorns",
"minecraft:depth_strider",
"minecraft:frost_walker",
"minecraft:binding_curse",
"minecraft:soul_speed",
"minecraft:swift_sneak",
"minecraft:sharpness",
"minecraft:smite",
"minecraft:bane_of_arthropods",
"minecraft:knockback",
"minecraft:fire_aspect",
"minecraft:looting",
"minecraft:sweeping_edge",
"minecraft:efficiency",
"minecraft:silk_touch",
"minecraft:unbreaking",
"minecraft:fortune",
"minecraft:power",
"minecraft:punch",
"minecraft:flame",
"minecraft:infinity",
"minecraft:luck_of_the_sea",
"minecraft:lure",
"minecraft:loyalty",
"minecraft:impaling",
"minecraft:riptide",
"minecraft:channeling",
"minecraft:multishot",
"minecraft:quick_charge",
"minecraft:piercing",
"minecraft:density",
"minecraft:breach",
"minecraft:wind_burst",
"minecraft:mending",
"minecraft:vanishing_curse"
]
]]>
</code-block>
</procedure>

If the json file was formated like the Mathematical Analysis (every Enchantment is an ordered pair of the index and the enchantment name), this is how it would look:

<procedure collapsible="true" title="enchantment_data.json Mathematical Analysis">

<code-block lang="json">
<![CDATA[
[
    {1, "minecraft:protection"},
    {2, "minecraft:fire_protection"},
    {3, "minecraft:feather_falling"},
    {4, "minecraft:blast_protection"},
    {5, "minecraft:projectile_protection"},
    {6, "minecraft:respiration"},
    {7, "minecraft:aqua_affinity"},
    {8, "minecraft:thorns"},
    {9, "minecraft:depth_strider"},
    {10, "minecraft:frost_walker"},
    {11, "minecraft:binding_curse"},
    {12, "minecraft:soul_speed"},
    {13, "minecraft:swift_sneak"},
    {14, "minecraft:sharpness"},
    {15, "minecraft:smite"},
    {16, "minecraft:bane_of_arthropods"},
    {17, "minecraft:knockback"},
    {18, "minecraft:fire_aspect"},
    {19, "minecraft:looting"},
    {20, "minecraft:sweeping_edge"},
    {21, "minecraft:efficiency"},
    {22, "minecraft:silk_touch"},
    {23, "minecraft:unbreaking"},
    {24, "minecraft:fortune"},
    {25, "minecraft:power"},
    {26, "minecraft:punch"},
    {27, "minecraft:flame"},
    {28, "minecraft:infinity"},
    {29, "minecraft:luck_of_the_sea"},
    {30, "minecraft:lure"},
    {31, "minecraft:loyalty"},
    {32, "minecraft:impaling"},
    {33, "minecraft:riptide"},
    {34, "minecraft:channeling"},
    {35, "minecraft:multishot"},
    {36, "minecraft:quick_charge"},
    {37, "minecraft:piercing"},
    {38, "minecraft:density"},
    {39, "minecraft:breach"},
    {40, "minecraft:wind_burst"},
    {41, "minecraft:mending"},
    {42, "minecraft:vanishing_curse"}
]
]]>
</code-block>
</procedure>

Based off of this representation, one list represents one complete graph of mutually exclusive enchantments:

<img width="700" src="4-cropped.svg">

Based on the Mathematical Analysis, the table in reality isn't quite generated as analysed.
The table is generated such that the enchantments are grouped together based on their compatibility with each other in a list.

Reason being is that it is more useful to have the enchantments grouped together based on their compatibility with each other,
it is more simple to configure.

<procedure title="mutually_exclusive_data.json">

<code-block lang="json">
<![CDATA[
[
  [
    "minecraft:protection",
    "minecraft:fire_protection",
    "minecraft:blast_protection",
    "minecraft:projectile_protection"
  ],
  [
    "minecraft:sharpness",
    "minecraft:smite",
    "minecraft:bane_of_arthropods"
  ],
  [
    "minecraft:depth_strider",
    "minecraft:frost_walker"
  ],
  [
    "minecraft:silk_touch",
    "minecraft:fortune"
  ],
  [
    "minecraft:multishot",
    "minecraft:piercing"
  ],
  [
    "minecraft:channeling",
    "minecraft:riptide"
  ]
]
]]>
</code-block>
</procedure>
