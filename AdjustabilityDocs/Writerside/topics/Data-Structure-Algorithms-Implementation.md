# Data Structures &amp; Algorithms Implementation

The data structure which was chosen to represent the mutually exclusive enchantments is a `HashMap`.

Using a structure like this is beneficial for several reasons, particularly in contexts where you need to quickly access mutually exclusive relationships or perform efficient lookups:

- **Efficient Lookup**:
    O(1) Time Complexity: HashMaps (or dictionaries) provide O(1) average-time complexity for lookups. This means that given an enchantment, you can quickly retrieve its list of mutually exclusive enchantments.
    Direct Access: You can directly access the list of mutually exclusive enchantments without having to iterate through other data structures, such as lists or sets, making the operation faster.

- **Explicit Relationships**: The map clearly represents the mutually exclusive relationships, making the code easier to understand and maintain. Each enchantment directly points to its exclusive counterparts.
    Self-Contained Information: Each key-value pair in the map provides complete information about the exclusivity for that specific enchantment, making the data self-contained.

- **Large Data Sets**: As the number of enchantments grows, the efficiency of lookups remains constant, making this approach scalable.
    Dynamic Updates: Adding or removing enchantments and their relationships can be done efficiently by updating the map.

- **Versatility**:
    Flexible Use Cases: This structure can be easily extended to other domains where mutually exclusive relationships need to be managed, not just enchantments in Minecraft.
    Complex Operations: Operations like checking if two enchantments are mutually exclusive, retrieving all conflicts for a given enchantment, and adding new enchantments can be performed efficiently.

## HashMap Construction Procedure

The `MutuallyExclusive` Class manages mutually exclusive enchantments and provides methods to check for their presence on an ItemStack.

<procedure title="MutuallyExclusive() Constructor">
<p>Constructs a HashMap <code>mutuallyExclusiveMap</code> from a graph structure that allows for constant time complexity (O(1)) for search operations in the best case.</p>
<p>The HashMap maps each Enchantment to a set of mutually exclusive Enchantments.
The construction process is as follows:</p>
<step>
    Iterates over each sub-graph in the provided graph structure.
</step>
<step>
For each Enchantment in a sub-graph:
<ol>
    <li>
        Creates a new HashSet containing all Enchantments in that sub-graph.
    </li>
    <li>
        Removes the current Enchantment from the HashSet.
    </li>
    <li>
        Uses the current Enchantment as the key and the modified HashSet as the value, then adds this entry to the `mutuallyExclusiveMap`.
    </li>
</ol>
</step>
</procedure>

<code-block collapsible="true" lang="java" collapsed-title="MutuallyExclusive Class">

    public class MutuallyExclusive
    {
        private final Map<Enchantment, Set<Enchantment>> mutuallyExclusiveMap = new HashMap<>();
            
        public MutuallyExclusive (List<List<Enchantment>> mutuallyExclusiveGraph)
        {
            for ( List<Enchantment> group : mutuallyExclusiveGraph )
            {
                for ( Enchantment enchantment : group )
                {
                    Set<Enchantment> exclusives = new HashSet<>(group);
                    exclusives.remove(enchantment);
                    mutuallyExclusiveMap.put(enchantment, exclusives);
                }
            }
        }
    }
    ...
</code-block>

## Example

Given the following graph structure:

```
[
    "minecraft:protection",
    "minecraft:fire_protection",
    "minecraft:blast_protection",
    "minecraft:projectile_protection"
]
```

The HashMap `mutuallyExclusiveMap` would be constructed as follows:

```
[
    "minecraft:protection" : [
        "minecraft:blast_protection",
        "minecraft:fire_protection",
        "minecraft:projectile_protection"
    ].
    "minecraft:fire_protection" : [
        "minecraft:protection",
        "minecraft:blast_protection",
        "minecraft:projectile_protection"
    ],
    "minecraft:blast_protection" : [
        "minecraft:protection",
        "minecraft:fire_protection",
        "minecraft:projectile_protection"
    ],
    "minecraft:projectile_protection" : [
        "minecraft:protection",
        "minecraft:fire_protection",
        "minecraft:blast_protection"
    ]
]
```
<procedure>
<note>
The `printMutuallyExclusiveMap()` method can be used to display the constructed HashMap for debugging purposes:
</note>
<code-block collapsible="true" collapsed-title="printMutuallyExclusiveMap() Console Output">
  enchantment.minecraft.smite : enchantment.minecraft.bane_of_arthropods enchantment.minecraft.sharpness
  enchantment.minecraft.blast_protection : enchantment.minecraft.protection enchantment.minecraft.fire_protection enchantment.minecraft.projectile_protection
  enchantment.minecraft.bane_of_arthropods : enchantment.minecraft.smite enchantment.minecraft.sharpness
  enchantment.minecraft.fire_protection : enchantment.minecraft.protection enchantment.minecraft.blast_protection enchantment.minecraft.projectile_protection
  enchantment.minecraft.frost_walker : enchantment.minecraft.depth_strider
  enchantment.minecraft.silk_touch : enchantment.minecraft.fortune
  enchantment.minecraft.sharpness : enchantment.minecraft.smite enchantment.minecraft.bane_of_arthropods
  enchantment.minecraft.protection : enchantment.minecraft.blast_protection enchantment.minecraft.fire_protection enchantment.minecraft.projectile_protection
  enchantment.minecraft.depth_strider : enchantment.minecraft.frost_walker
  enchantment.minecraft.projectile_protection : enchantment.minecraft.protection enchantment.minecraft.blast_protection enchantment.minecraft.fire_protection
  enchantment.minecraft.riptide : enchantment.minecraft.channeling
  enchantment.minecraft.piercing : enchantment.minecraft.multishot
  enchantment.minecraft.multishot : enchantment.minecraft.piercing
  enchantment.minecraft.fortune : enchantment.minecraft.silk_touch
  enchantment.minecraft.channeling : enchantment.minecraft.riptide
</code-block>
</procedure>





## HashMap String Elements Analysis

The total number of string elements in the HashMap is the square of the total number of string elements in the initial list. This can be demonstrated as follows:

Let `n` be the number of enchantments in the initial list.

Let `m` be the number of enchantments in the HashMap, including both the Keys and Values.

```tex
m = n^2
```
Consider a List of Enchantments which contains `n` enchantments.

### Analysis
1. **Number of Keys**:
    - The number of keys in the HashMap is equal to the number of enchantments in the initial list. If the list contains `n` enchantments, there will be `n` keys.
    - For the Protection sub-graph example: 4 keys.

2. **Number of Values (Mutually Exclusive Sets)**:
    - Each key maps to a set of `n-1` mutually exclusive enchantments.
    - In this example: Each key maps to a set of 3 values.

3. **Total Number of Individual String Elements**:
    - Each key appears once as a key and `n-1` times as a value in the sets of other keys.
    - Therefore, each enchantment appears `n` times in total (once as a key and `n-1` times as a value).

4. **Total Count**:
    - For `n` enchantments, the total number of individual string elements is `n^2`

### Example Calculation
For an initial list of 4 enchantments:
- Number of keys: 4
- Each key appears in 3 sets (values).
- Total appearances of each enchantment: 4 (once as a key + 3 times as a value).

Thus, the total number of individual string elements is:
4 enchantments * 4 appearances per enchantment = 16 total individual string elements

### Conclusion
Therefore, the total number of string elements in the HashMap is the square of the total number of string elements in the initial list. If there are `n` enchantments in the initial list, the total number of individual string elements in the HashMap will be `n^2`.
