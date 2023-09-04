# folio-pickslip-viewer

Callslip (pickslip) viewer and printing application for FOLIO.

This is Spring app for displaying and printing pickslips to aid stack retrieval.

This application maintains a single in-memory model of requests / callslips
at each stack location, which is updated every minute during peak times, and every
five minutes at other times.  This model is consulted when rendering browser pages,
and supplemented (with additional information from FOLIO) when generating printed 
callslips.  

Stack locations are configured in the stacklocations-spec.yml file.

## Dependencies

This application makes calls to FOLIO via the folio-api.  It has no database or
other dependencies.

## Printing

Callslips (pickslips) can be printed via PDF files.  Pickslips are printed in 
two halves, aligned to a performation in the paper.  Each pickslip is printed
to a single PDF using Flying Saucer with Open PDF.  Multiple pickslips can be
printed (in which case single page PDF files are stitched together).

(Flying Saucer supports enough of CSS 2.1 to work well, but doesn't support
multi page printing with the type of exact layout around perforated pages
we rely on.)

### Fonts

Flying Saucer does not support any type of font fallback for rendering unicode
characters or foreign script (e.g. CJK) characters.  See comments in 
unicode-font-map.yml for information on installing and configuring fonts to
support non arabic unicode glyphs.


