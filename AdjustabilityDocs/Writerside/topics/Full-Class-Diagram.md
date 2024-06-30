# Full Class Diagram

<chapter collapsible="true" title="Access Modifier Notation">
<p>Characters that define the visibility:</p>
<table>
<tr><th>Character</th><th>Icon for field</th><th>Icon for method</th><th>Visibility</th></tr>
<tr><td><code>-</code></td><td>&#9723;</td><td>&#9724;</td><td><code>private</code></td></tr>
<tr><td><code>#</code></td><td>&#9671;</td><td>&#9670;</td><td><code>protected</code></td></tr>
<tr><td><code>~</code></td><td>&#9651;</td><td>&#9650;</td><td><code>package private</code></td></tr>
<tr><td><code>+</code></td><td>&#9675;</td><td>&#9679;</td><td><code>public</code></td></tr>
</table>
</chapter>

```plantuml
@startuml
top to bottom direction
package adjustability
{

package render_core {


class RenderCore {
+ RenderCore()
+ drawRectangle(PoseStack, int, int, int, int, int, int, int): void
+ drawGradientRectangle(Matrix4f, int, int, int, int, int, int, int): void
+ drawBoxBorders(PoseStack, int, int, int, int, int, int, boolean, int): void
+ drawGradientRectangle(Matrix4f, BufferBuilder, int, int, int, int, int, int, int): void
+ renderText(String, float, float, int, int, boolean, GuiGraphics): void
}
}

package enchantments_logic {
class AnvilHandler {
+ AnvilHandler()
+ getEnchantmentsList(ItemStack): List<Enchantment>
+ getEnchantment(ItemStack): Enchantment?
+ onAnvilUpdate(AnvilUpdateEvent): void
- hasEnchantments(ItemStack): boolean
- areMutuallyExclusive(ItemStack, ItemStack): boolean
}
}

class Adjustability {
+ Adjustability()
- LOGGER: Logger
+ MODID: String
- commonSetup(FMLCommonSetupEvent): void
  }

class Config {
+ Config()
- BUILDER: Builder
  ~ SPEC: ForgeConfigSpec
  }
  
  package gui_elements{
class Button {
  + Button(int, int, int, int, int, String): 
  + Button(): 
  - x: int
  - width: int
  - text: String
  - height: int
  - y: int
  - bgColor: int
  + render(PoseStack): void
}
}
class CustomTooltip {
+ CustomTooltip()
~ BACKGROUND_COLOR: int
~ PADDING: int
~ MOUSE_OFFSET: int
~ x: int
~ BLENDED_COLOR: int
~ BORDER_COLOR_TOP: int
~ y: int
~ BORDER_COLOR_BOTTOM: int
~ rendering: boolean
+ drawBoxBorders(Render): void
+ drawBaseRectangleWithBorders(Render, Vector2ic): void
+ cancelDefaultTooltip(RenderTooltipEvent): void
+ blockScreenInputMouseClick(Pre): void
+ positionTooltip(int, int, Vector2i, int, int): void
+ addBottomInformationToTheDefaultTooltipForOpeningTheCustomTooltipGui(ItemTooltipEvent): void
+ letGoOfKey(KeyReleased): void
+ onClickTickWise(MouseButton): void
+ blockScreenInputMouseDragged(Pre): void
+ blockScreenInputMouseScroll(Pre): void
+ mouseEventHelper(int, int, int, int): boolean
- shouldRender(int, int): boolean
+ drawDecorationsAroundTooltip(Render, Vector2ic): void
+ positionTooltip(int, int, int, int, int, int): Vector2ic
+ blockScreenInputMouseClickRelease(Pre): void
}
  }
  



Adjustability --[#595959,dashed]-> AnvilHandler : "«create»"
Adjustability --[#595959,dashed]-> CustomTooltip : "«create»"
Config --[#595959,dashed]-> Adjustability
CustomTooltip --[#595959,dashed]-> Adjustability
CustomTooltip --> RenderCore : <<uses>>
CustomTooltip --[#595959,dashed]-> Button : "«create»"
Button --> RenderCore : <<uses>>
@enduml
```