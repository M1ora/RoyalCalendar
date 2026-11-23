# Clock composition prototype

React mockup of the approved Figma composition, not an Android implementation.

```jsx
import RoyalClockMockup from "./RoyalClockMockup.jsx";

<RoyalClockMockup />
// Unknown weather: hide the icon and its plate without moving the other layers.
<RoyalClockMockup showWeather={false} />
```

The date and time are fixed to the reference: January 12, Friday, Night, 03:21.
The component is transparent and scales uniformly within its container.
It includes the supplied artwork; existing artwork attribution in ASSETS.md applies.
