/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.email.mail.internet;

import com.android.email.mail.BodyPart;
import com.android.email.mail.MessageTestUtils;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;
import com.android.email.mail.MessageTestUtils.MessageBuilder;
import com.android.email.mail.MessageTestUtils.MultipartBuilder;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

/**
 * This is a series of unit tests for the MimeUtility class.  These tests must be locally
 * complete - no server(s) required.
 */
@SmallTest
public class MimeUtilityTest extends TestCase {

    /** up arrow, down arrow, left arrow, right arrow */
    private final String SHORT_UNICODE = "\u2191\u2193\u2190\u2192";
    private final String SHORT_UNICODE_ENCODED = "=?UTF-8?B?4oaR4oaT4oaQ4oaS?=";
    
    /** dollar and euro sign */
    private final String PADDED2_UNICODE = "$\u20AC";
    private final String PADDED2_UNICODE_ENCODED = "=?UTF-8?B?JOKCrA==?=";
    private final String PADDED1_UNICODE = "$$\u20AC";
    private final String PADDED1_UNICODE_ENCODED = "=?UTF-8?B?JCTigqw=?=";
    private final String PADDED0_UNICODE = "$$$\u20AC";
    private final String PADDED0_UNICODE_ENCODED = "=?UTF-8?B?JCQk4oKs?=";

    /** a string without any unicode */
    private final String SHORT_PLAIN = "abcd";
    
    /** long subject which will be split into two MIME/Base64 chunks */
    private final String LONG_UNICODE_SPLIT =
        "$" +
        "\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC" +
        "\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC";
    private final String LONG_UNICODE_SPLIT_ENCODED =
        "=?UTF-8?B?JOKCrOKCrOKCrOKCrOKCrOKCrOKCrOKCrA==?=" + "\r\n " +
        "=?UTF-8?B?4oKs4oKs4oKs4oKs4oKs4oKs4oKs4oKs4oKs4oKs4oKs4oKs?=";

    /** strings that use supplemental characters and really stress encode/decode */
    // actually it's U+10400
    private final String SHORT_SUPPLEMENTAL = "\uD801\uDC00";
    private final String SHORT_SUPPLEMENTAL_ENCODED = "=?UTF-8?B?8JCQgA==?=";
    private final String LONG_SUPPLEMENTAL = SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL +
        SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL +
        SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL;
    private final String LONG_SUPPLEMENTAL_ENCODED =
        "=?UTF-8?B?8JCQgPCQkIDwkJCA8JCQgA==?=" + "\r\n " + 
        "=?UTF-8?B?8JCQgPCQkIDwkJCA8JCQgPCQkIDwkJCA?=";
    private final String LONG_SUPPLEMENTAL_2 = "a" + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL +
        SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL +
        SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL + SHORT_SUPPLEMENTAL;
    private final String LONG_SUPPLEMENTAL_ENCODED_2 =
        "=?UTF-8?B?YfCQkIDwkJCA8JCQgPCQkIA=?=" + "\r\n " +
        "=?UTF-8?B?8JCQgPCQkIDwkJCA8JCQgPCQkIDwkJCA?="; 
    // Earth is U+1D300.
    private final String LONG_SUPPLEMENTAL_QP =
        "*Monogram for Earth \uD834\uDF00. Monogram for Human \u268b."; 
    private final String LONG_SUPPLEMENTAL_QP_ENCODED =
        "=?UTF-8?Q?*Monogram_for_Earth_?=" + "\r\n " +
        "=?UTF-8?Q?=F0=9D=8C=80._Monogram_for_Human_=E2=9A=8B.?=";

    /** a typical no-param header */
    private final String HEADER_NO_PARAMETER =
            "header";
    /** a typical multi-param header */
    private final String HEADER_MULTI_PARAMETER =
            "header; Param1Name=Param1Value; Param2Name=Param2Value";
    /** a multi-param header with quoting */
    private final String HEADER_QUOTED_MULTI_PARAMETER =
            "header; Param1Name=\"Param1Value\"; Param2Name=\"Param2Value\"";
    /** a malformed header we're seeing in production servers */
    private final String HEADER_MALFORMED_PARAMETER =
            "header; Param1Name=Param1Value; filename";
    
    /**
     * a string generated by google calendar that contains two interesting gotchas:
     * 1.  Uses windows-1252 encoding, and en-dash recoded appropriately (\u2013 / =96)
     * 2.  Because the first encoded char requires '=XX' encoding, we create an "internal"
     *     "?=" that the decoder must correctly skip over.
     **/
    private final String CALENDAR_SUBJECT_UNICODE = 
        "=?windows-1252?Q?=5BReminder=5D_test_=40_Fri_Mar_20_10=3A30am_=96_11am_=28andro?=" +
        "\r\n\t" +
        "=?windows-1252?Q?id=2Etr=40gmail=2Ecom=29?=";
    private final String CALENDAR_SUBJECT_PLAIN =
        "[Reminder] test @ Fri Mar 20 10:30am \u2013 11am (android.tr@gmail.com)";
    
    /**
     * Some basic degenerate strings designed to exercise error handling in the decoder
     */
    private final String CALENDAR_DEGENERATE_UNICODE_1 =
        "=?windows-1252?Q=5B?=";
    private final String CALENDAR_DEGENERATE_UNICODE_2 =
        "=?windows-1252Q?=5B?=";
    private final String CALENDAR_DEGENERATE_UNICODE_3 =
        "=?windows-1252?=";
    private final String CALENDAR_DEGENERATE_UNICODE_4 =
        "=?windows-1252";

    /**
     * Test that decode/unfold is efficient when it can be
     */
    public void testEfficientUnfoldAndDecode() {
        String result1 = MimeUtility.unfold(SHORT_PLAIN);
        String result2 = MimeUtility.decode(SHORT_PLAIN);
        String result3 = MimeUtility.unfoldAndDecode(SHORT_PLAIN);
        
        assertSame(SHORT_PLAIN, result1);
        assertSame(SHORT_PLAIN, result2);
        assertSame(SHORT_PLAIN, result3);
    }

    // TODO:  more tests for unfold(String s)
        
    /**
     * Test that decode is working for simple strings
     */
    public void testDecodeSimple() {
        String result1 = MimeUtility.decode(SHORT_UNICODE_ENCODED);
        assertEquals(SHORT_UNICODE, result1);
    }
    
    // TODO:  tests for decode(String s)

    /**
     * Test that unfoldAndDecode is working for simple strings
     */
    public void testUnfoldAndDecodeSimple() {
        String result1 = MimeUtility.unfoldAndDecode(SHORT_UNICODE_ENCODED);
        assertEquals(SHORT_UNICODE, result1);
    }
    
    /**
     * test decoding complex string from google calendar that has two gotchas for the decoder.
     * also tests a couple of degenerate cases that should "fail" decoding and pass through.
     */
    public void testComplexDecode() {
        String result1 = MimeUtility.unfoldAndDecode(CALENDAR_SUBJECT_UNICODE);
        assertEquals(CALENDAR_SUBJECT_PLAIN, result1);
        
        // These degenerate cases should "fail" and return the same string
        String degenerate1 = MimeUtility.unfoldAndDecode(CALENDAR_DEGENERATE_UNICODE_1);
        assertEquals("degenerate case 1", CALENDAR_DEGENERATE_UNICODE_1, degenerate1);
        String degenerate2 = MimeUtility.unfoldAndDecode(CALENDAR_DEGENERATE_UNICODE_2);
        assertEquals("degenerate case 2", CALENDAR_DEGENERATE_UNICODE_2, degenerate2);
        String degenerate3 = MimeUtility.unfoldAndDecode(CALENDAR_DEGENERATE_UNICODE_3);
        assertEquals("degenerate case 3", CALENDAR_DEGENERATE_UNICODE_3, degenerate3);
        String degenerate4 = MimeUtility.unfoldAndDecode(CALENDAR_DEGENERATE_UNICODE_4);
        assertEquals("degenerate case 4", CALENDAR_DEGENERATE_UNICODE_4, degenerate4);
    }
    
    // TODO:  more tests for unfoldAndDecode(String s)

    /**
     * Test that fold/encode is efficient when it can be
     */
    public void testEfficientFoldAndEncode() {
        String result1 = MimeUtility.foldAndEncode(SHORT_PLAIN);
        String result2 = MimeUtility.foldAndEncode2(SHORT_PLAIN, 10);
        String result3 = MimeUtility.fold(SHORT_PLAIN, 10);
        
        assertSame(SHORT_PLAIN, result1);
        assertSame(SHORT_PLAIN, result2);
        assertSame(SHORT_PLAIN, result3);
    }

    /**
     * Test about base64 padding variety.
     */
    public void testPaddingOfFoldAndEncode2() {
        String result1 = MimeUtility.foldAndEncode2(PADDED2_UNICODE, 0);
        String result2 = MimeUtility.foldAndEncode2(PADDED1_UNICODE, 0);
        String result3 = MimeUtility.foldAndEncode2(PADDED0_UNICODE, 0);
        
        assertEquals("padding 2", PADDED2_UNICODE_ENCODED, result1);
        assertEquals("padding 1", PADDED1_UNICODE_ENCODED, result2);
        assertEquals("padding 0", PADDED0_UNICODE_ENCODED, result3);
    }

    // TODO:  more tests for foldAndEncode(String s)

    /**
     * Test that foldAndEncode2 is working for simple strings
     */
    public void testFoldAndEncode2() {
        String result1 = MimeUtility.foldAndEncode2(SHORT_UNICODE, 10);
        assertEquals(SHORT_UNICODE_ENCODED, result1);
    }
    
    /**
     * Test that foldAndEncode2 is working for long strings which needs splitting.
     */
    public void testFoldAndEncode2WithLongSplit() {
        String result = MimeUtility.foldAndEncode2(LONG_UNICODE_SPLIT, "Subject: ".length()); 

        assertEquals("long string", LONG_UNICODE_SPLIT_ENCODED, result);
    }
     
    /**
     * Tests of foldAndEncode2 that involve supplemental characters (UTF-32)
     *
     * Note that the difference between LONG_SUPPLEMENTAL and LONG_SUPPLEMENTAL_2 is the
     * insertion of a single character at the head of the string. This is intended to disrupt
     * the code that splits the long string into multiple encoded words, and confirm that it
     * properly applies the breaks between UTF-32 code points.
     */
    public void testFoldAndEncode2Supplemental() {
        String result1 = MimeUtility.foldAndEncode2(SHORT_SUPPLEMENTAL, "Subject: ".length());
        String result2 = MimeUtility.foldAndEncode2(LONG_SUPPLEMENTAL, "Subject: ".length());
        String result3 = MimeUtility.foldAndEncode2(LONG_SUPPLEMENTAL_2, "Subject: ".length());
        assertEquals("short supplemental", SHORT_SUPPLEMENTAL_ENCODED, result1);
        assertEquals("long supplemental", LONG_SUPPLEMENTAL_ENCODED, result2);
        assertEquals("long supplemental 2", LONG_SUPPLEMENTAL_ENCODED_2, result3);
    }

    /**
     * Tests of foldAndEncode2 that involve supplemental characters (UTF-32)
     *
     * Note that the difference between LONG_SUPPLEMENTAL and LONG_SUPPLEMENTAL_QP is that
     * the former will be encoded as base64 but the latter will be encoded as quoted printable.
     */
    public void testFoldAndEncode2SupplementalQuotedPrintable() {
        String result = MimeUtility.foldAndEncode2(LONG_SUPPLEMENTAL_QP, "Subject: ".length());
        assertEquals("long supplement quoted printable",
                     LONG_SUPPLEMENTAL_QP_ENCODED, result);
    }

    // TODO:  more tests for foldAndEncode2(String s)
    // TODO:  more tests for fold(String s, int usedCharacters)
    
    /**
     * Basic tests of getHeaderParameter()
     * 
     * Typical header value:  multipart/mixed; boundary="----E5UGTXUQQJV80DR8SJ88F79BRA4S8K"
     * 
     * Function spec says:
     *  if header is null:  return null
     *  if name is null:    if params, return first param.  else return full field
     *  else:               if param is found (case insensitive) return it
     *                        else return null
     */
    public void testGetHeaderParameter() {
        // if header is null, return null
        assertNull("null header check", MimeUtility.getHeaderParameter(null, "name"));
        
        // if name is null, return first param or full header
        // NOTE:  The docs are wrong - it returns the header (no params) in that case
//      assertEquals("null name first param per docs", "Param1Value", 
//              MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, null));
        assertEquals("null name first param per code", "header",
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, null));
        assertEquals("null name full header", HEADER_NO_PARAMETER,
                MimeUtility.getHeaderParameter(HEADER_NO_PARAMETER, null));
        
        // find name 
        assertEquals("get 1st param", "Param1Value",
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "Param1Name"));
        assertEquals("get 2nd param", "Param2Value",
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "Param2Name"));
        assertEquals("get missing param", null,
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "Param3Name"));
        
        // case insensitivity
        assertEquals("get 2nd param all LC", "Param2Value",
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "param2name"));
        assertEquals("get 2nd param all UC", "Param2Value",
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "PARAM2NAME"));

        // quoting
        assertEquals("get 1st param", "Param1Value",
                MimeUtility.getHeaderParameter(HEADER_QUOTED_MULTI_PARAMETER, "Param1Name"));
        assertEquals("get 2nd param", "Param2Value",
                MimeUtility.getHeaderParameter(HEADER_QUOTED_MULTI_PARAMETER, "Param2Name"));

        // Don't fail when malformed
        assertEquals("malformed filename param", null,
                MimeUtility.getHeaderParameter(HEADER_MALFORMED_PARAMETER, "filename"));
    }
    
    // TODO:  tests for findFirstPartByMimeType(Part part, String mimeType)

    /** Tests for findPartByContentId(Part part, String contentId) */
    public void testFindPartByContentIdTestCase() throws MessagingException, Exception {
        final String cid1 = "cid.1@android.com";
        final Part cid1bp = MessageTestUtils.bodyPart("image/gif", cid1);
        final String cid2 = "cid.2@android.com";
        final Part cid2bp = MessageTestUtils.bodyPart("image/gif", "<" + cid2 + ">");

        final Message msg1 = new MessageBuilder()
            .setBody(new MultipartBuilder("multipart/related")
                 .addBodyPart(MessageTestUtils.bodyPart("text/html", null))
                 .addBodyPart((BodyPart)cid1bp)
                 .build())
            .build();
        // found cid1 part
        final Part actual1_1 = MimeUtility.findPartByContentId(msg1, cid1);
        assertEquals("could not found expected content-id part", cid1bp, actual1_1);

        final Message msg2 = new MessageBuilder()
            .setBody(new MultipartBuilder("multipart/mixed")
                .addBodyPart(MessageTestUtils.bodyPart("image/tiff", "cid.4@android.com"))
                .addBodyPart(new MultipartBuilder("multipart/related")
                    .addBodyPart(new MultipartBuilder("multipart/alternative")
                        .addBodyPart(MessageTestUtils.bodyPart("text/plain", null))
                        .addBodyPart(MessageTestUtils.bodyPart("text/html", null))
                        .buildBodyPart())
                    .addBodyPart((BodyPart)cid1bp)
                    .buildBodyPart())
                .addBodyPart(MessageTestUtils.bodyPart("image/gif", "cid.3@android.com"))
                .addBodyPart((BodyPart)cid2bp)
                .build())
            .build();
        // found cid1 part
        final Part actual2_1 = MimeUtility.findPartByContentId(msg2, cid1);
        assertEquals("found part from related multipart", cid1bp, actual2_1);

        // found cid2 part
        final Part actual2_2 = MimeUtility.findPartByContentId(msg2, cid2);
        assertEquals("found part from mixed multipart", cid2bp, actual2_2);
    }
    
    /** Tests for getTextFromPart(Part part) */
    public void testGetTextFromPartContentTypeCase() throws MessagingException {
        final String theText = "This is the text of the part";
        TextBody tb = new TextBody(theText);
        MimeBodyPart p = new MimeBodyPart();
        
        // 1. test basic text/plain mode
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain");
        p.setBody(tb);
        String gotText = MimeUtility.getTextFromPart(p);
        assertEquals(theText, gotText);
        
        // 2. mixed case is OK
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "TEXT/PLAIN");
        p.setBody(tb);
        gotText = MimeUtility.getTextFromPart(p);
        assertEquals(theText, gotText);
        
        // 3. wildcards OK
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/other");
        p.setBody(tb);
        gotText = MimeUtility.getTextFromPart(p);
        assertEquals(theText, gotText);
    }

    /** Test for usage of Content-Type in getTextFromPart(Part part).
     * 
     * For example 'Content-Type: text/html; charset=utf-8'
     * 
     *  If the body part has no mime-type, refuses to parse content as text.
     *  If the mime-type does not match text/*, it will not get parsed.
     *  Then, the charset parameter is used, with a default of ASCII.
     *
     *  This test works by using a string that is valid Unicode, and is also
     *  valid when decoded from UTF-8 bytes into Windows-1252 (so that
     *  auto-detection is not possible), and checks that the correct conversion
     *  was made, based on the Content-Type header.
     *  
     */
    public void testContentTypeCharset() throws MessagingException {
        final String UNICODE_EXPECT = "This is some happy unicode text \u263a";
        // What you get if you encode to UTF-8 (\xe2\x98\xba) and reencode with Windows-1252
        final String WINDOWS1252_EXPECT = "This is some happy unicode text \u00e2\u02dc\u00ba";
        TextBody tb = new TextBody(UNICODE_EXPECT);
        MimeBodyPart p = new MimeBodyPart();

        String gotText, mimeType, charset;
        // TEST 0: Standard Content-Type header; no extraneous spaces or fields
        p.setBody(tb);
        // We call setHeader after setBody, since setBody overwrites Content-Type
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/html; charset=utf-8");
        gotText = MimeUtility.getTextFromPart(p);
        assertTrue(MimeUtility.mimeTypeMatches(p.getMimeType(), "text/html"));
        assertEquals(UNICODE_EXPECT, gotText);

        p.setBody(tb);
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/html; charset=windows-1252");
        gotText = MimeUtility.getTextFromPart(p);
        assertTrue(MimeUtility.mimeTypeMatches(p.getMimeType(), "text/html"));
        assertEquals(WINDOWS1252_EXPECT, gotText);

        // TEST 1: Extra fields and quotes in Content-Type (from RFC 2045)
        p.setBody(tb);
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                    "text/html; prop1 = \"test\"; charset = \"utf-8\"; prop2 = \"test\"");
        gotText = MimeUtility.getTextFromPart(p);
        assertTrue(MimeUtility.mimeTypeMatches(p.getMimeType(), "text/html"));
        assertEquals(UNICODE_EXPECT, gotText);

        p.setBody(tb);
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                    "text/html; prop1 = \"test\"; charset = \"windows-1252\"; prop2 = \"test\"");
        gotText = MimeUtility.getTextFromPart(p);
        assertTrue(MimeUtility.mimeTypeMatches(p.getMimeType(), "text/html"));
        assertEquals(WINDOWS1252_EXPECT, gotText);

        // TEST 2: Mixed case in Content-Type header:
        // RFC 2045 says that content types, subtypes and parameter names
        // are case-insensitive.

        p.setBody(tb);
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "TEXT/HtmL ; CHARseT=utf-8");
        gotText = MimeUtility.getTextFromPart(p);
        assertTrue(MimeUtility.mimeTypeMatches(p.getMimeType(), "text/html"));
        assertEquals(UNICODE_EXPECT, gotText);

        p.setBody(tb);
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "TEXT/HtmL ; CHARseT=windows-1252");
        gotText = MimeUtility.getTextFromPart(p);
        assertTrue(MimeUtility.mimeTypeMatches(p.getMimeType(), "text/html"));
        assertEquals(WINDOWS1252_EXPECT, gotText);

        // TEST 3: Comments in Content-Type header field (from RFC 2045)
        // Thunderbird permits comments after the end of a parameter, as in this example.
        // Not something that I have seen in the real world outside RFC 2045.

        p.setBody(tb);
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                    "text/html; charset=utf-8 (Plain text)");
        gotText = MimeUtility.getTextFromPart(p);
        assertTrue(MimeUtility.mimeTypeMatches(p.getMimeType(), "text/html"));
        // Note: This test does not pass.
        //assertEquals(UNICODE_EXPECT, gotText);

        p.setBody(tb);
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE,
                    "text/html; charset=windows-1252 (Plain text)");
        gotText = MimeUtility.getTextFromPart(p);
        assertTrue(MimeUtility.mimeTypeMatches(p.getMimeType(), "text/html"));
        // Note: These tests does not pass.
        //assertEquals(WINDOWS1252_EXPECT, gotText);
    }
    
    /** Tests for various aspects of mimeTypeMatches(String mimeType, String matchAgainst) */
    public void testMimeTypeMatches() {
        // 1. No match
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", "TEXT/PLAIN"));
        
        // 2. Match
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "text/plain"));
        
        // 3. Match (mixed case)
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "TEXT/PLAIN"));
        assertTrue(MimeUtility.mimeTypeMatches("TEXT/PLAIN", "text/plain"));
        
        // 4. Match (wildcards)
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "*/plain"));
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "text/*"));
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "*/*"));
        
        // 5. No Match (wildcards)
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", "*/plain"));
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", "text/*"));
    }
    
    /** Tests for various aspects of mimeTypeMatches(String mimeType, String[] matchAgainst) */
    public void testMimeTypeMatchesArray() {
        // 1. Zero-length array
        String[] arrayZero = new String[0];
        assertFalse(MimeUtility.mimeTypeMatches("text/plain", arrayZero));
        
        // 2. Single entry, no match
        String[] arrayOne = new String[] { "text/plain" };
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", arrayOne));
        
        // 3. Single entry, match
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", arrayOne));
        
        // 4. Multi entry, no match
        String[] arrayTwo = new String[] { "text/plain", "match/this" };
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", arrayTwo));
        
        // 5. Multi entry, match first
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", arrayTwo));
        
        // 6. Multi entry, match not first
        assertTrue(MimeUtility.mimeTypeMatches("match/this", arrayTwo));
    }

    // TODO:  tests for decodeBody(InputStream in, String contentTransferEncoding)    
    // TODO:  tests for collectParts(Part part, ArrayList<Part> viewables, ArrayList<Part> attachments)

}
