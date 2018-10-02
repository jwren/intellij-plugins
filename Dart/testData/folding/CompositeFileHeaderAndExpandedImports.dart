
<fold text='/.../' expand='false'>// foo

/**
 * bar
 */

/// baz

/*
s
 */</fold>

library foo;

export <fold text='...' expand='true'>"dart:core";
<fold text='/*...*/' expand='true'>/* comment */</fold>
// comment
<fold text='///...' expand='true'>/// doc</fold>
<fold text='/**...*/' expand='true'>/**
 * doc
 */</fold>
import "dart:core";

import "";

export "";</fold>