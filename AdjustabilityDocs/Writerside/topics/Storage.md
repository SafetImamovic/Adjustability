# Storage

## Main Functionality

**Adjustability**'s main functionality is to enable the Player to toggle between **Mutually Exclusive Enchantments**   of an **Item** (**ItemStack** Class), of course if that particlar item meets that condition.

<procedure title="Example">

If an item, for instance Diamond Pickaxe, is enchanted with 2 Mutually Exclusive Enchantments, let's say **Fortune III** and **Silk Touch**, the mod should have a mechanism which chooses one of those Enchantments (More details on how that's chosen) to be active on the item (because Mutually Exclusive Enchantments shouldn't stack).

So, some sort of **disable(_ItemStack item_)** method would be really helpful.

The issue is the way **Minecraft** and the **Forge API** handle Enchantments.

</procedure>



## Default Enchantment Handling

Now, there isn't anything inherently wrong with the way Enchantments are handled by default, all of the **EnchantmentHandler** methods do their job as intended, but for the purposes of **Adjustability** it leaves a lot to be desired.

If you wanted to '_disable_' an enchantment on an item, there's no direct way of doing that. The **EnchantmentHandler** class doesn't provide such methods. Instead, if you wanted '_disable_' a certain enchantment you would need to completely remove that enchantment from the item, essentially '_disabling_' it.


Now, You might think:

'Why don't you just create a custom enchantment class that inherits from the existing **Enchantment** class and implement that funcionality?'

I don't like that solution because then the mod would have to copy over the objects from the **Enchantment** class and instatiate objects based off of my **CustomEnchantment** class. This just adds unecessary object's onto the memory and creates a middle-man between enchantable entities and base enchantments.

<procedure title="Possible Solution">

In this scenario, if some other mod wanted to interact with the enchantments on the item, they would have to go through the **CustomEnchantment** class, which is not ideal.

<code-block lang="plantuml">
<![CDATA[

    @startuml
    top to bottom direction
    skinparam linetype ortho
    
    
    entity Cost << record >> {
    }
    class CustomEnchantment {
    + CustomEnchantment(EnchantmentDefinition):
      + disable(ItemStack): void
        }
        class Enchantment
        entity EnchantmentDefinition << record >>
        interface FeatureElement << interface >>
        interface IForgeEnchantment << interface >>
    
    CustomEnchantment      ---[#000082,plain]-^  Enchantment
    
    Cost                   +-[#820000,plain]-  Enchantment           
    Enchantment            -[#008200,dashed]-^  FeatureElement        
    Enchantment            -[#008200,dashed]-^  IForgeEnchantment     
    EnchantmentDefinition  +-[#820000,plain]-  Enchantment
    @enduml
]]>
</code-block>

If that mod implemented 2 Enchantments which are Mutually Exclusive,
they would have to be copied over to my **CustomEnchantment** class and then the
**disable(_ItemStack item_)** method would be called when needed.

<code-block lang="plantuml">
<![CDATA[

    @startuml
    
    top to bottom direction
    
    skinparam linetype ortho
    
    class CustomEnchantment {
    + CustomEnchantment(EnchantmentDefinition):
      + disable(ItemStack): void
        }
        class Enchantment
        class ItemStack
        {
        +getEnchantments()
        }
    
    CustomEnchantment      ---[#000082,plain]-^  Enchantment
    Enchantment         --[#595959,dashed]->  CustomEnchantment : "«create»"
    
    @enduml

]]>
</code-block>

</procedure>


The approach is to come up with the least invasive way to handle data.

One way, which is actually pretty invasive, is to persist the Mutually Exclusive Enchantment data again to the **ItemStack** NBT data.

**ItemStack** `ComponentData`
```
{enchantments: [
    {silk_touch},
    {fortune},
    {efficiency}
  ]
}

...rest of the nbt data...

+  {enchantments: [
+      {silk_touch},
+      {fortune}
+    ]
+  }
```

Then this **ItemStack** will persist the data that's necessary for **Adjustability**.

The downside is of course that this approach is very invasive, because this data is persisted at the vanilla level. Meaning that if a player removes **Adjustability**.jar file, the additional enchantment data will be persisted forever.



## Solution

The way most other mods handle data is by persisting data in their own custom directories, creating a custom database.

This is the approach that was chosen because:
- Non-invasive - No vanilla data is affected

This is possible because each **ItemStack** object has a unique identifier (it's not universally unique like a UUID, it's unique in the context of the server/game instance), we can make entries to our custom '_table_' which stores the different mutually exclusive enchantments based off of the **ItemStack** it's asosciated with.

<procedure title="ER Diagram">

    
<code-block lang="plantuml">
    <![CDATA[
    @startuml

    ' hide the spot
    ' hide circle
    
    ' avoid problems with angled crows feet
    skinparam linetype ortho
    
    entity "CMEED" as e01 {
    *ID : number <<generated>>
    --
    *DATA : text
    }
    
    entity "DISD" as e02 {
    *ID : number <<generated>>
    --
    *DATA : text
    }
    
    entity "ISMEED" as e03 {
    *ID : number <<generated>>
    --
    *ItemStack_ID : number <<FK>>
    *constant_MEED_ID : number <<FK>>
    }
    
    
    e01 }|..|| e03
    e02 }|..|| e03
    
    @enduml
    ]]>
</code-block>

</procedure>



| **Table Abbreviation** | **Table Name**                                                         |
|------------------------|------------------------------------------------------------------------|
| **CMEED**              | **C**onstant **M**utually **E**xclusive **E**nchantment **D**ata       |
| **ISMEED**             | **I**tem **S**tack **M**utually **E**xclusive **E**nchantment **D**ata |
| **DISD**               | **D**efault **I**tem **S**tack **D**ata                                |

The **CMEED** (**C**onstant **M**utually **E**xclusive **E**nchantment **D**ata) table will store the different Mutually Exclusive Enchantments that are available in the mod.

| **ID** | **Data**                   |
|--------|----------------------------|
| **1**  | `Silk Touch`               |
| **2**  | `Fortune`                  |
| **3**  | `Protection`               |
| **4**  | `Blast Protection`         |
| **n**  | **n**-th Enchantment Entry |

where:

<code-block lang="tex" >
n\epsilon \mathbb{N}
</code-block>

<procedure title="Practical Example">
<step>
    There exists a <strong>Diamond Pickaxe</strong> with exactly <strong>1</strong> Enchantment:
    <ol>
        <li><strong>Silk Touch</strong></li>
    </ol>
</step>
<step>
    The <strong>ItemStack</strong> object for this <strong>Diamond Pickaxe</strong> will have a unique identifier, let's say <strong>1</strong>.
</step>
<step>
    The player then <strong>Enchants</strong> that Diamond Pickaxe with <strong>Fortune III</strong>.
</step>
<step>
    Now there exists a <strong>Diamond Pickaxe</strong> with <strong>2</strong> Mutually Exclusive Enchantments:
    <ol>
        <li><strong>Silk Touch</strong></li>
        <li><strong>Fortune III</strong></li>
    </ol>
</step>
<step>
    Because the action of <strong>Enchanting</strong> the Diamond Pickaxe with <strong>Fortune III</strong> is a <strong>Game Event</strong>, our static function is fired 
    which checks if the <strong>ItemStack</strong> object with the unique identifier <strong>1</strong> has any <strong>Potential Mutually Exclusive Enchantments</strong>.
</step>
<step>
    <strong>IF</strong> 
</step>
</procedure>

<procedure title="Technical ER Diagram">
<code-block lang="plantuml">
    <![CDATA[

    @startuml
    !define Table(x) class x << (T, #FFAAAA) >>
    !define PrimaryKey(x) <b>x</b>
    !define ForeignKey(x) <u>x</u>
    
    Table(Enchantments) {
        << PrimaryKey(enchantment_id) >>
        + enchantment_id : int
        + name : string
        + description : string
    }
    
    Table(MutuallyExclusiveEnchantments) {
        << PrimaryKey(pair_id) >>
        + pair_id : int
        << ForeignKey(enchantment_id_1) >>
        + enchantment_id_1 : int
        << ForeignKey(enchantment_id_2) >>
        + enchantment_id_2 : int
        + reason : string
    }
    
    Table(ItemStacks) {
        << PrimaryKey(itemstack_id) >>
        + itemstack_id : int
        + item_name : string
    }
    
    Table(EnchantmentsInItemStack) {
        << PrimaryKey(itemstack_enchantment_id) >>
        + itemstack_enchantment_id : int
        << ForeignKey(itemstack_id) >>
        + itemstack_id : int
        << ForeignKey(enchantment_id) >>
        + enchantment_id : int
    }
    
    Enchantments ||..|{ MutuallyExclusiveEnchantments
    ItemStacks ||..|{ EnchantmentsInItemStack
    Enchantments ||..|{ EnchantmentsInItemStack
    
    @enduml
    ]]>
</code-block>
</procedure>




<procedure title="Classical ER Diagram">
<code-block lang="plantuml">
    <![CDATA[
    @startuml

    ' hide the spot
    ' hide circle
    
    ' avoid problems with angled crows feet
    
    entity "Enchantments" as e01 {
    *enchantment_id : number <<generated>>
    --
    *name: string
    *description: string
    }

    entity "MutuallyExclusiveEnchantments" as e04 {
    *pair_id : number <<generated>>
    --
    *enchantment_id_1 : number <<FK>>
    *enchantment_id_2 : number <<FK>>
    *reason : string
    }
    
    entity "ItemStacks" as e02 {
    *itemstack_id : number <<generated>>
    --
    *item_name : string
    }
    
    entity "EnchantmentsInItemStack" as e03 {
    *itemstack_enchantment_id : number <<generated>>
    --
    *itemstack_id : number <<FK>>
    *enchantment_id : number <<FK>>
    }

    

    e01 "1" -- "m" e04
    e01 "1" -- "n" e03
    e02 "1" -- "o" e03
    
    @enduml
    ]]>
</code-block>
    <p>where:</p>

```tex
m,n,o \epsilon \mathbb{N}\cup \{0\}
```
</procedure>


