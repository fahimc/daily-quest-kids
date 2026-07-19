# Accessibility

Daily Quest Kids accessibility requirements:

- TalkBack labels for all puzzle cards, navigation controls and result states.
- Minimum 48dp touch targets.
- Text remains readable at 200% scale.
- High-contrast colour tokens.
- Reduced-motion setting for non-essential animation.
- Colour-independent feedback using labels, shape, iconography and borders.
- No countdown pressure, guilt messaging or social comparison.

Current implementation:

- Home cards expose semantic descriptions and button roles.
- The design system includes category colours plus borders and text labels.
- The UI uses system fonts rather than bundled unlicensed fonts.

Pending:

- Full TalkBack traversal pass.
- Automated touch-target checks.
- 200% text-scale VRT.
- High-contrast VRT.
