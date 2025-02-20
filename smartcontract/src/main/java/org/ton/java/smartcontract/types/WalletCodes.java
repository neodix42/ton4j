package org.ton.java.smartcontract.types;

public enum WalletCodes {
  V1R1(
      "B5EE9C72410101010044000084FF0020DDA4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED5441FDF089"),
  V1R2(
      "B5EE9C724101010100530000A2FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54D0E2786F"),
  V1R3(
      "B5EE9C7241010101005F0000BAFF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54B5B86E42"),
  V2R1(
      "B5EE9C724101010100570000AAFF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54A1370BB6"),
  V2R2(
      "B5EE9C724101010100630000C2FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54044CD7A1"),
  V3R1(
      "B5EE9C724101010100620000C0FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED543FBE6EE0"),
  V3R2(
      "B5EE9C724101010100710000DEFF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED5410BD6DAD"),
  V4R2(
      "B5EE9C72410214010002D4000114FF00F4A413F4BCF2C80B010201200203020148040504F8F28308D71820D31FD31FD31F02F823BBF264ED44D0D31FD31FD3FFF404D15143BAF2A15151BAF2A205F901541064F910F2A3F80024A4C8CB1F5240CB1F5230CBFF5210F400C9ED54F80F01D30721C0009F6C519320D74A96D307D402FB00E830E021C001E30021C002E30001C0039130E30D03A4C8CB1F12CB1FCBFF1011121302E6D001D0D3032171B0925F04E022D749C120925F04E002D31F218210706C7567BD22821064737472BDB0925F05E003FA403020FA4401C8CA07CBFFC9D0ED44D0810140D721F404305C810108F40A6FA131B3925F07E005D33FC8258210706C7567BA923830E30D03821064737472BA925F06E30D06070201200809007801FA00F40430F8276F2230500AA121BEF2E0508210706C7567831EB17080185004CB0526CF1658FA0219F400CB6917CB1F5260CB3F20C98040FB0006008A5004810108F45930ED44D0810140D720C801CF16F400C9ED540172B08E23821064737472831EB17080185005CB055003CF1623FA0213CB6ACB1FCB3FC98040FB00925F03E20201200A0B0059BD242B6F6A2684080A06B90FA0218470D4080847A4937D29910CE6903E9FF9837812801B7810148987159F31840201580C0D0011B8C97ED44D0D70B1F8003DB29DFB513420405035C87D010C00B23281F2FFF274006040423D029BE84C600201200E0F0019ADCE76A26840206B90EB85FFC00019AF1DF6A26840106B90EB858FC0006ED207FA00D4D422F90005C8CA0715CBFFC9D077748018C8CB05CB0222CF165005FA0214CB6B12CCCCC973FB00C84014810108F451F2A7020070810108D718FA00D33FC8542047810108F451F2A782106E6F746570748018C8CB05CB025006CF165004FA0214CB6A12CB1FCB3FC973FB0002006C810108D718FA00D33F305224810108F459F2A782106473747270748018C8CB05CB025005CF165003FA0213CB6ACB1F12CB3FC973FB00000AF400C9ED54696225E5"),
  V5R1(
      "B5EE9C7241021401000281000114FF00F4A413F4BCF2C80B01020120020D020148030402DCD020D749C120915B8F6320D70B1F2082106578746EBD21821073696E74BDB0925F03E082106578746EBA8EB48020D72101D074D721FA4030FA44F828FA443058BD915BE0ED44D0810141D721F4058307F40E6FA1319130E18040D721707FDB3CE03120D749810280B99130E070E2100F020120050C020120060902016E07080019ADCE76A2684020EB90EB85FFC00019AF1DF6A2684010EB90EB858FC00201480A0B0017B325FB51341C75C875C2C7E00011B262FB513435C280200019BE5F0F6A2684080A0EB90FA02C0102F20E011E20D70B1F82107369676EBAF2E08A7F0F01E68EF0EDA2EDFB218308D722028308D723208020D721D31FD31FD31FED44D0D200D31F20D31FD3FFD70A000AF90140CCF9109A28945F0ADB31E1F2C087DF02B35007B0F2D0845125BAF2E0855036BAF2E086F823BBF2D0882292F800DE01A47FC8CA00CB1F01CF16C9ED542092F80FDE70DB3CD81003F6EDA2EDFB02F404216E926C218E4C0221D73930709421C700B38E2D01D72820761E436C20D749C008F2E09320D74AC002F2E09320D71D06C712C2005230B0F2D089D74CD7393001A4E86C128407BBF2E093D74AC000F2E093ED55E2D20001C000915BE0EBD72C08142091709601D72C081C12E25210B1E30F20D74A111213009601FA4001FA44F828FA443058BAF2E091ED44D0810141D718F405049D7FC8CA0040048307F453F2E08B8E14038307F45BF2E08C22D70A00216E01B3B0F2D090E2C85003CF1612F400C9ED54007230D72C08248E2D21F2E092D200ED44D0D2005113BAF2D08F54503091319C01810140D721D70A00F2E08EE2C8CA0058CF16C9ED5493F2C08DE20010935BDB31E1D74CD0B4D6C35E"),
  lockup(
      "B5EE9C7241021E01000261000114FF00F4A413F4BCF2C80B010201200203020148040501F2F28308D71820D31FD31FD31F802403F823BB13F2F2F003802251A9BA1AF2F4802351B7BA1BF2F4801F0BF9015410C5F9101AF2F4F8005057F823F0065098F823F0062071289320D74A8E8BD30731D4511BDB3C12B001E8309229A0DF72FB02069320D74A96D307D402FB00E8D103A4476814154330F004ED541D0202CD0607020120131402012008090201200F100201200A0B002D5ED44D0D31FD31FD3FFD3FFF404FA00F404FA00F404D1803F7007434C0C05C6C2497C0F83E900C0871C02497C0F80074C7C87040A497C1383C00D46D3C00608420BABE7114AC2F6C2497C338200A208420BABE7106EE86BCBD20084AE0840EE6B2802FBCBD01E0C235C62008087E4055040DBE4404BCBD34C7E00A60840DCEAA7D04EE84BCBD34C034C7CC0078C3C412040DD78CA00C0D0E00130875D27D2A1BE95B0C60000C1039480AF00500161037410AF0050810575056001010244300F004ED540201201112004548E1E228020F4966FA520933023BB9131E2209835FA00D113A14013926C21E2B3E6308003502323287C5F287C572FFC4F2FFFD00007E80BD00007E80BD00326000431448A814C4E0083D039BE865BE803444E800A44C38B21400FE809004E0083D10C06002012015160015BDE9F780188242F847800C02012017180201481B1C002DB5187E006D88868A82609E00C6207E00C63F04EDE20B30020158191A0017ADCE76A268699F98EB85FFC00017AC78F6A268698F98EB858FC00011B325FB513435C2C7E00017B1D1BE08E0804230FB50F620002801D0D3030178B0925B7FE0FA4031FA403001F001A80EDAA4"),
  dnsCollection(
      "B5EE9C7241021D010002C7000114FF00F4A413F4BCF2C80B0102016202030202CC040502012017180201200607020120131402012008090201200D0E016D420C70094840FF2F0DE01D0D3030171B0925F03E0FA403001D31FED44D0D4D4303122C000E30210245F048210370FEC51BADC840FF2F080A0201200B0C00D032F82320821062E44069BCF2E0C701F00420D74920C218F2E0C8208103F0BBF2E0C92078A908C000F2E0CA21F005F2E0CB58F00714BEF2E0CC22F9018050F833206EB38E10D0F4043052108307F40E6FA131F2D0CD9130E2C85004CF16C9C85003CF1612CCC9F00C000D1C3232C072742000331C27C074C1C07000082CE500A98200B784B98C4830003CB432600201200F100201201112004F3223880875D244B5C61673C58875D2883000082CE6C070007CB83280B50C3400A44C78B98C727420007F1C0875D2638D572E882CE38B8C00B4C1C8700B48F0802C0929BE14902E6C08B08BC8F04EAC2C48B09800F05EC4EC04AC6CC82CE500A98200B784F7B99B04AEA00093083001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C600069006AC0BC018060840EE6B2802A0060840EE6B2802A00A08418B9101A68608209E3402A410830856456F81B04A5A9D6A0192A41392002015815160039D2CF8053810F805BBC00C646582AC678B387D0165B5E66664C0207D804002D007232FFFE0A33C5B25C083232C044FD003D0032C03260001B3E401D3232C084B281F2FFF27420020120191A0201201B1C0007B8B5D318001FBA7A3ED44D0D4D43031F00A7001F00B8001BB905BED44D0D4D430307FF002128009DBA30C3020D74978A908C000F2E04620D70A07C00021D749C0085210B0935B786DE0209501D3073101DE21F0035122D71830F9018200BA93C8CB0F01820167A3ED43D8CF16C90191789170E212A0018F83DF327"),
  dnsItem(
      "B5EE9C7241022801000698000114FF00F4A413F4BCF2C80B0102016202030202CC04050201201E1F02012006070201481819020120080902015816170201200A0B000D470C8CB01C9D0801F73E09DBC400B434C0C05C6C2497C1383E903E900C7E800C5C75C87E800C7E800C3C0289ECE39397C15B088D148CB1C17CB865407E90350C1B5C3232C1FD00327E08E08418B9101A68608209E3402A4108308324CC200337A0404B20403C162A20032A41287E08C0683C00911DFC02440D7E08FC02F814D671C1462C200C00113E910C1C2EBCB8536003F88E34109B5F0BFA40307020F8256D8040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00E029C70091709509D31F50AAE221F008F82321BC24C0008E9E343A3A3B8E1636363737375135C705F2E196102510241023F823F00BE30EE0310DD33F256EB31FB0926C21E30D0D0E0F00FE302680698064A98452B0BEF2E19782103B9ACA0052A0A15270BC993682103B9ACA0019A193390805E220C2008E328210557CEA20F82510396D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00923036E2803C23F823A1A120C2009313A0029130E24474F0091024F823F00B00D2343653CDA182103B9ACA005210A15270BC993682103B9ACA0016A1923005E220C2008E378210370FEC516D72295134544743708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB001CA10B9130E26D5477655477632EF00B0204C882105FCC3D145220BA8E9531373B5372C705F2E191109A104910384706401504E082101A0B9D515220BA8E195B32353537375135C705F2E19A03D4304015045033F823F00BE02182104EB1F0F9BAE3023B20821044BEAE41BAE302382782104ED14B65BA1310111200885B363638385147C705F2E19B04D3FF20D74AC20007D0D30701C000F2E19CF404300798D43040168307F417983050058307F45B30E270C8CB07F400C910354014F823F00B01FE30363A246EF2E19D8050F833D0F4043052408307F40E6FA1F2E19FD30721C00022C001B1F2E1A021C0008E9124109B1068517A10571046105C43144CDD9630103A395F07E201C0018E32708210370FEC51586D8100A0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00915BE21301FE8E7A37F8235006A1810258BC066E16B0F2E19E23D0D749F823F0075290BEF2E1975178A182103B9ACA00A120C2008E32102782104ED14B6558076D72708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093303535E2F82381012CA0F0024477F0091045103412F823F00BE05F041501F03502FA4021F001FA40D20031FA0082103B9ACA001DA121945314A0A1DE22D70B01C300209205A19135E220C2FFF2E192218E3E821005138D91C8500BCF16500DCF1671244B145448C0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00106994102C395BE20114008A8E3528F0018210D53276DB103946096D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093383430E21045103412F823F00B009A32353582102FCB26A2BA8E3A7082108B77173504C8CBFF5005CF161443308040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00E05F04840FF2F00093083001258C2040FA201938083001658C20407D200CB8083001A58C204064200A38083001E58C20404B2007B8083002258C204032200538083002650C20191EB83002A4E00C9D781E9C600069006AC0BC018060840EE6B2802A0060840EE6B2802A00A08418B9101A68608209E3402A410830856456F81B04A5A9D6A0192A4139200201201A1B0201201C1D0021081BA50C1B5C0838343E903E8034CFCC200017321400F3C5807E80B2CFF26000513B513434FFFE900835D2708027DFC07E9035353D0134CFCC0415C415B80C1C1B5B5B5B490415C415A0002B01B232FFD40173C59400F3C5B3333D0032CFF27B5520020120202102012024250013BBB39F00A175F07F008802027422230010A874F00A10475F07000CA959F00A6C71000DB8FCFF00A5F03802012026270013B64A5E014204EBE0FA1000C7B461843AE9240F152118001E5C08DE014206EBE0FA1A60E038001E5C339E8086007AE140F8001E5C33B84111C466105E033E04883DCB11FB64DDC4964AD1BA06B879240DC23572F37CC5CAAAB143A2FFFBC4180012660F003C003060FE81EDF4260F00306EB1583C"),
  jettonMinter(
      "B5EE9C7241020B010001ED000114FF00F4A413F4BCF2C80B0102016202030202CC040502037A60090A03EFD9910E38048ADF068698180B8D848ADF07D201800E98FE99FF6A2687D007D206A6A18400AA9385D47181A9AA8AAE382F9702480FD207D006A18106840306B90FD001812881A28217804502A906428027D012C678B666664F6AA7041083DEECBEF29385D71811A92E001F1811802600271812F82C207F97840607080093DFC142201B82A1009AA0A01E428027D012C678B00E78B666491646580897A007A00658064907C80383A6465816503E5FFE4E83BC00C646582AC678B28027D0109E5B589666664B8FD80400FE3603FA00FA40F82854120870542013541403C85004FA0258CF1601CF16CCC922C8CB0112F400F400CB00C9F9007074C8CB02CA07CBFFC9D05008C705F2E04A12A1035024C85004FA0258CF16CCCCC9ED5401FA403020D70B01C3008E1F8210D53276DB708010C8CB055003CF1622FA0212CB6ACB1FCB3FC98042FB00915BE200303515C705F2E049FA403059C85004FA0258CF16CCCCC9ED54002E5143C705F2E049D43001C85004FA0258CF16CCCCC9ED54007DADBCF6A2687D007D206A6A183618FC1400B82A1009AA0A01E428027D012C678B00E78B666491646580897A007A00658064FC80383A6465816503E5FFE4E840001FAF16F6A2687D007D206A6A183FAA904051007F09"),
  jettonWallet(
      "B5EE9C7241021201000328000114FF00F4A413F4BCF2C80B0102016202030202CC0405001BA0F605DA89A1F401F481F481A8610201D40607020148080900BB0831C02497C138007434C0C05C6C2544D7C0FC02F83E903E900C7E800C5C75C87E800C7E800C00B4C7E08403E29FA954882EA54C4D167C0238208405E3514654882EA58C511100FC02780D60841657C1EF2EA4D67C02B817C12103FCBC2000113E910C1C2EBCB853600201200A0B020120101101F500F4CFFE803E90087C007B51343E803E903E90350C144DA8548AB1C17CB8B04A30BFFCB8B0950D109C150804D50500F214013E809633C58073C5B33248B232C044BD003D0032C032483E401C1D3232C0B281F2FFF274013E903D010C7E801DE0063232C1540233C59C3E8085F2DAC4F3208405E351467232C7C6600C03F73B51343E803E903E90350C0234CFFE80145468017E903E9014D6F1C1551CDB5C150804D50500F214013E809633C58073C5B33248B232C044BD003D0032C0327E401C1D3232C0B281F2FFF274140371C1472C7CB8B0C2BE80146A2860822625A020822625A004AD822860822625A028062849F8C3C975C2C070C008E00D0E0F009ACB3F5007FA0222CF165006CF1625FA025003CF16C95005CC2391729171E25008A813A08208989680AA008208989680A0A014BCF2E2C504C98040FB001023C85004FA0258CF1601CF16CCC9ED5400705279A018A182107362D09CC8CB1F5230CB3F58FA025007CF165007CF16C9718018C8CB0524CF165006FA0215CB6A14CCC971FB0010241023000E10491038375F040076C200B08E218210D53276DB708010C8CB055008CF165004FA0216CB6A12CB1F12CB3FC972FB0093356C21E203C85004FA0258CF1601CF16CCC9ED5400DB3B51343E803E903E90350C01F4CFFE803E900C145468549271C17CB8B049F0BFFCB8B0A0822625A02A8005A805AF3CB8B0E0841EF765F7B232C7C572CFD400FE8088B3C58073C5B25C60063232C14933C59C3E80B2DAB33260103EC01004F214013E809633C58073C5B3327B55200083200835C87B51343E803E903E90350C0134C7E08405E3514654882EA0841EF765F784EE84AC7CB8B174CFCC7E800C04E81408F214013E809633C58073C5B3327B55205ECCF23D"),
  jettonMinterStableCoin(
      "B5EE9C72010218010005BB000114FF00F4A413F4BCF2C80B0102016202030202CB0405020120141502F3D0CB434C0C05C6C238ECC200835C874C7C0608405E351466EA44C38601035C87E800C3B51343E803E903E90353534541168504D3214017E809400F3C58073C5B333327B55383E903E900C7E800C7D007E800C7E80004C5C3E0E80B4C7C04074CFC044BB51343E803E903E9035353449A084190ADF41EEB8C089A0607001DA23864658380E78B64814183FA0BC0019635355161C705F2E04904FA4021FA4430C000F2E14DFA00D4D120D0D31F018210178D4519BAF2E0488040D721FA00FA4031FA4031FA0020D70B009AD74BC00101C001B0F2B19130E254431B0803FA82107BDD97DEBA8EE7363805FA00FA40F82854120A70546004131503C8CB0358FA0201CF1601CF16C921C8CB0113F40012F400CB00C9F9007074C8CB02CA07CBFFC9D05008C705F2E04A12A14414506603C85005FA025003CF1601CF16CCCCC9ED54FA40D120D70B01C000B3915BE30DE02682102C76B973BAE30235250A0B0C018E2191729171E2F839206E938124279120E2216E94318128739101E25023A813A0738103A370F83CA00270F83612A00170F836A07381040982100966018070F837A0BCF2B025597F0900EC82103B9ACA0070FB02F828450470546004131503C8CB0358FA0201CF1601CF16C921C8CB0113F40012F400CB00C920F9007074C8CB02CA07CBFFC9D0C8801801CB0501CF1658FA02029858775003CB6BCCCC9730017158CB6ACCE2C98011FB005005A04314C85005FA025003CF1601CF16CCCCC9ED540044C8801001CB0501CF1670FA027001CB6A8210D53276DB01CB1F0101CB3FC98042FB0001FC145F04323401FA40D2000101D195C821CF16C9916DE2C8801001CB055004CF1670FA027001CB6A8210D173540001CB1F500401CB3F23FA4430C0008E35F828440470546004131503C8CB0358FA0201CF1601CF16C921C8CB0113F40012F400CB00C9F9007074C8CB02CA07CBFFC9D012CF1697316C127001CB01E2F400C90D04F882106501F354BA8E223134365145C705F2E04902FA40D1103402C85005FA025003CF1601CF16CCCCC9ED54E0258210FB88E119BA8E2132343603D15131C705F2E0498B025512C85005FA025003CF1601CF16CCCCC9ED54E034248210235CAF52BAE30237238210CB862902BAE302365B2082102508D66ABAE3026C310E0F101100088050FB0002EC3031325033C705F2E049FA40FA00D4D120D0D31F01018040D7212182100F8A7EA5BA8E4D36208210595F07BCBA8E2C3004FA0031FA4031F401D120F839206E943081169FDE718102F270F8380170F836A0811A7770F836A0BCF2B08E138210EED236D3BA9504D30331D19434F2C048E2E2E30D50037012130044335142C705F2E049C85003CF16C9134440C85005FA025003CF1601CF16CCCCC9ED54001E3002C705F2E049D4D4D101ED54FB0400188210D372158CBADC840FF2F000CE31FA0031FA4031FA4031F401FA0020D70B009AD74BC00101C001B0F2B19130E25442162191729171E2F839206E938124279120E2216E94318128739101E25023A813A0738103A370F83CA00270F83612A00170F836A07381040982100966018070F837A0BCF2B000C082103B9ACA0070FB02F828450470546004131503C8CB0358FA0201CF1601CF16C921C8CB0113F40012F400CB00C920F9007074C8CB02CA07CBFFC9D0C8801801CB0501CF1658FA02029858775003CB6BCCCC9730017158CB6ACCE2C98011FB000025BD9ADF6A2687D007D207D206A6A6888122F82402027116170085ADBCF6A2687D007D207D206A6A688A2F827C1400B82A3002098A81E46581AC7D0100E78B00E78B6490E4658089FA00097A00658064FC80383A6465816503E5FFE4E84000CFAF16F6A2687D007D207D206A6A68BF99E836C1783872EBDB514D9C97C283B7F0AE5179029E2B6119C39462719E4F46ED8F7413E62C780A417877407E978F01A40711411B1ACB773A96BDD93FA83BB5CA8435013C8C4B3AC91F4589B4780A38646583FA0064A18040"),
  jettonWalletStableCoin(
      "B5EE9C7201020F010003D1000114FF00F4A413F4BCF2C80B01020162020302F8D001D0D3030171B08E48135F038020D721ED44D0D303FA00FA40FA40D104D31F01840F218210178D4519BA0282107BDD97DEBA12B1F2F48040D721FA003012A0401303C8CB0358FA0201CF1601CF16C9ED54E0FA40FA4031FA0031F401FA0031FA00013170F83A02D31F012082100F8A7EA5BA8E85303459DB3CE03304050201200D0E01F203D33F0101FA00FA4021FA4430C000F2E14DED44D0D303FA00FA40FA40D15309C7052471B0C00021B1F2AD522BC705500AB1F2E0495115A120C2FFF2AFF82A54259070546004131503C8CB0358FA0201CF1601CF16C921C8CB0113F40012F400CB00C920F9007074C8CB02CA07CBFFC9D004FA40F401FA00200602D0228210178D4519BA8E84325ADB3CE034218210595F07BCBA8E843101DB3CE032208210EED236D3BA8E2F30018040D721D303D1ED44D0D303FA00FA40FA40D1335142C705F2E04A403303C8CB0358FA0201CF1601CF16C9ED54E06C218210D372158CBADC840FF2F00809019820D70B009AD74BC00101C001B0F2B19130E2C88210178D451901CB1F500A01CB3F5008FA0223CF1601CF1626FA025007CF16C9C8801801CB055004CF1670FA024063775003CB6BCCCCC945370700B42191729171E2F839206E938124279120E2216E94318128739101E25023A813A0738103A370F83CA00270F83612A00170F836A07381040982100966018070F837A0BCF2B0048050FB005803C8CB0358FA0201CF1601CF16C9ED5403F4ED44D0D303FA00FA40FA40D12372B0C002F26D07D33F0101FA005141A004FA40FA4053BAC705F82A5464E070546004131503C8CB0358FA0201CF1601CF16C921C8CB0113F40012F400CB00C9F9007074C8CB02CA07CBFFC9D0500CC7051BB1F2E04A09FA0021925F04E30D26D70B01C000B393306C33E30D55020A0B0C01F2ED44D0D303FA00FA40FA40D106D33F0101FA00FA40F401D15141A15288C705F2E04926C2FFF2AFC882107BDD97DE01CB1F5801CB3F01FA0221CF1658CF16C9C8801801CB0526CF1670FA02017158CB6ACCC903F839206E943081169FDE718102F270F8380170F836A0811A7770F836A0BCF2B0028050FB00030C0060C882107362D09C01CB1F2501CB3F5004FA0258CF1658CF16C9C8801001CB0524CF1658FA02017158CB6ACCC98011FB00007A5054A1F82FA07381040982100966018070F837B60972FB02C8801001CB055005CF1670FA027001CB6A8210D53276DB01CB1F5801CB3FC9810082FB0059002003C8CB0358FA0201CF1601CF16C9ED540027BFD8176A2686981FD007D207D206899FC15209840021BC508F6A2686981FD007D207D2068AF81C"),
  nftItem(
      "B5EE9C7241020D010001D0000114FF00F4A413F4BCF2C80B0102016202030202CE04050009A11F9FE00502012006070201200B0C02D70C8871C02497C0F83434C0C05C6C2497C0F83E903E900C7E800C5C75C87E800C7E800C3C00812CE3850C1B088D148CB1C17CB865407E90350C0408FC00F801B4C7F4CFE08417F30F45148C2EA3A1CC840DD78C9004F80C0D0D0D4D60840BF2C9A884AEB8C097C12103FCBC20080900113E910C1C2EBCB8536001F65135C705F2E191FA4021F001FA40D20031FA00820AFAF0801BA121945315A0A1DE22D70B01C300209206A19136E220C2FFF2E192218E3E821005138D91C85009CF16500BCF16712449145446A0708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB00104794102A375BE20A00727082108B77173505C8CBFF5004CF1610248040708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB000082028E3526F0018210D53276DB103744006D71708010C8CB055007CF165005FA0215CB6A12CB1FCB3F226EB39458CF17019132E201C901FB0093303234E25502F003003B3B513434CFFE900835D27080269FC07E90350C04090408F80C1C165B5B60001D00F232CFD633C58073C5B3327B5520BF75041B"),
  nftCollection(
      "B5EE9C724102140100021F000114FF00F4A413F4BCF2C80B0102016202030202CD04050201200E0F04E7D10638048ADF000E8698180B8D848ADF07D201800E98FE99FF6A2687D20699FEA6A6A184108349E9CA829405D47141BAF8280E8410854658056B84008646582A802E78B127D010A65B509E58FE59F80E78B64C0207D80701B28B9E382F970C892E000F18112E001718112E001F181181981E0024060708090201200A0B00603502D33F5313BBF2E1925313BA01FA00D43028103459F0068E1201A44343C85005CF1613CB3FCCCCCCC9ED54925F05E200A6357003D4308E378040F4966FA5208E2906A4208100FABE93F2C18FDE81019321A05325BBF2F402FA00D43022544B30F00623BA9302A402DE04926C21E2B3E6303250444313C85005CF1613CB3FCCCCCCC9ED54002C323401FA40304144C85005CF1613CB3FCCCCCCC9ED54003C8E15D4D43010344130C85005CF1613CB3FCCCCCCC9ED54E05F04840FF2F00201200C0D003D45AF0047021F005778018C8CB0558CF165004FA0213CB6B12CCCCC971FB008002D007232CFFE0A33C5B25C083232C044FD003D0032C03260001B3E401D3232C084B281F2FFF2742002012010110025BC82DF6A2687D20699FEA6A6A182DE86A182C40043B8B5D31ED44D0FA40D33FD4D4D43010245F04D0D431D430D071C8CB0701CF16CCC980201201213002FB5DAFDA89A1F481A67FA9A9A860D883A1A61FA61FF480610002DB4F47DA89A1F481A67FA9A9A86028BE09E008E003E00B01A500C6E"),
  payments(
      "B5EE9C72410230010007FB000114FF00F4A413F4BCF2C80B0102012002030201480405000AF26C21F0190202CB06070201202E2F020120080902012016170201200A0B0201200C0D0009D3610F80CC001D6B5007434C7FE8034C7CC1BC0FE19E0201580E0F0201201011002D3E11DBC4BE11DBC43232C7FE11DBC47E80B2C7F2407320008B083E1B7B51343480007E187E80007E18BE80007E18F4FFC07E1934FFC07E1974DFC07E19BC01887080A7F4C7C07E1A34C7C07E1A7D01007E1AB7807080E535007E1AF7BE1B2002012012130201201415008D3E13723E11BE117E113E10540132803E10BE80BE10FE8084F2FFC4B2FFF2DFFC02887080A7FE12BE127E121400F2C7C4B2C7FD0037807080E53E12C073253E1333C5B8B27B5520004D1C3C02FE106CFCB8193E803E800C3E1096283E18BE10C0683E18FE10BE10E8006EFCB819BC032000CF1D3C02FE106CFCB819348020C235C6083E4040E4BE1124BE117890CC3E443CB81974C7C060841A5B9A5D2EBCB81A3E118074DFD66EBCB81CBE803E800C3E1094882FBE10D4882FAC3CB819807E18BE18FE12F43E800C3E10BE10E80068006E7CB8199FFE187C0320004120843777222E9C20043232C15401B3C594013E808532DA84B2C7F2DFF2407EC02002012018190201D42B2C0201201A1B0201201E1F0201201C1D00E5473F00BD401D001D401D021F90102D31F01821043436D74BAF2E068F84601D37F59BAF2E072F844544355F910F8454330F910B0F2E065D33FD33F30F84822B9F84922B9B0F2E06C21F86820F869F84A6E915B8E19F84AD0D33FFA003171D721D33F305033BC02BCB1936DF86ADEE2F800F00C8006F3E12F43E800C7E903E900C3E09DBC41CBE10D62F24CC20C1B7BE10FE11963C03FE10BE11A04020BC03DC3E185C3E189C3E18DB7E1ABC032000B51D3C02F5007400750074087E4040B4C7C0608410DB1BDCEEBCB81A3E118074DFD66EBCB81CBE111510D57E443E1150CC3E442C3CB8197E80007E18BE80007E18F4CFF4CFCC3E1208AE7E1248AE6C3CB81B007E1A3E1A7E003C042001C1573F00BF84A6EF2E06AD2008308D71820F9012392F84492F845E24130F910F2E065D31F018210556E436CBAF2E068F84601D37F59BAF2E072D401D08308D71820F901F8444130F910F2E06501D430D08308D71820F901F8454130F910F2E06501820020120222301FED31F01821043685374BAF2E068F84601D37F59BAF2E072D33FFA00F404552003D200019AD401D0D33FFA00F40430937F206DE2303205D31F01821043685374BAF2E068F84601D37F59BAF2E072D33FFA00F404552003D200019AD401D0D33FFA00F40430937F206DE23032F8485280BEF8495250BEB0524BBE1AB0527ABE19210064B05215BE14B05248BE17B0F2E06970F82305C8CB3F5004FA0215F40015CB3F5004FA0212F400CB1F12CA00CA00C9F86AF00C01C31CFC02FE129BACFCB81AF48020C235C6083E4048E4BE1124BE1178904C3E443CB81974C7C0608410DA19D46EBCB81A3E118074DFD66EBCB81CB5007420C235C6083E407E11104C3E443CB81940750C3420C235C6083E407E11504C3E443CB81940602403F71CFC02FE129BACFCB81AF48020C235C6083E4048E4BE1124BE1178904C3E443CB81974C7C0608410DB10DBAEBCB81A3E118074DFD66EBCB81CBD010C3E12B434CFFE803D0134CFFE803D0134C7FE11DBC4148828083E08EE7CB81BBE11DBC4A83E08EF3CB81C34800C151D5A64D6D4C8F7A2B98E82A49B08B8C3816028292A01FCD31F01821043685374BAF2E068F84601D37F59BAF2E072D33FFA00F404552003D200019AD401D0D33FFA00F40430937F206DE2303205D31F01821043685374BAF2E068F84601D37F59BAF2E072D33FFA00F404552003D200019AD401D0D33FFA00F40430937F206DE230325339BE5381BEB0F8495250BEB0F8485290BEB02502FE5237BE16B05262BEB0F2E06927C20097F84918BEF2E0699137E222C20097F84813BEF2E0699132E2F84AD0D33FFA00F404D33FFA00F404D31FF8476F105220A0F823BCF2E06FD200D20030B3F2E073209C3537373A5274BC5263BC12B18E11323939395250BC5299BC18B14650134440E25319BAB3F2E06D9130E30D7F05C82627002496F8476F1114A098F8476F1117A00603E203003ECB3F5004FA0215F40012CB3F5004FA0213F400CB1F12CA00CA00C9F86AF00C00620A8020F4966FA5208E213050038020F4666FA1208E1001FA00ED1E15DA119450C3A00B9133E2923430E202926C21E2B31B000C3535075063140038C8CB3F5004FA0212F400CB3F5003FA0213F400CB1FCA00C9F86AF00C00D51D3C02FE129BACFCB81AFE12B434CFFE803D010C74CFFE803D010C74C7CC3E11DBC4283E11DBC4A83E08EE7CB81C7E003E10886808E87E18BE10D400E816287E18FE10F04026BE10BE10E83E189C3E18F7BE10B04026BE10FE10A83E18DC3E18F780693E1A293E1A7C042001F53B7EF4C7C8608419F1F4A06EA4CC7C037808608403818830AEA54C7C03B6CC780C882084155DD61FAEA54C3C0476CC780820841E6849BBEEA54C3C04B6CC7808208407C546B3EEA54C3C0576CC780820840223AA8CAEA54C3C05B6CC7808208419BDBC1A6EA54C3C05F6CC780C60840950CAA46EA53C0636CC78202D0008840FF2F00075BC7FE3A7805FC25E87D007D207D20184100D0CAF6A1EC7C217C21B7817C227C22B7817C237C23FC247C24B7817C2524C3B7818823881B22A021984008DBD0CABA7805FC20C8B870FC253748B8F07C256840206B90FD0018C020EB90FD0018B8EB90E98F987C23B7882908507C11DE491839707C23B788507C23B789507C11DE48B9F03A4331C4966"),
  highload(
      "B5EE9C724101090100E5000114FF00F4A413F4BCF2C80B010201200203020148040501EAF28308D71820D31FD33FF823AA1F5320B9F263ED44D0D31FD33FD3FFF404D153608040F40E6FA131F2605173BAF2A207F901541087F910F2A302F404D1F8007F8E16218010F4786FA5209802D307D43001FB009132E201B3E65B8325A1C840348040F4438AE63101C8CB1F13CB3FCBFFF400C9ED54080004D03002012006070017BD9CE76A26869AF98EB85FFC0041BE5F976A268698F98E99FE9FF98FA0268A91040207A0737D098C92DBFC95DD1F140034208040F4966FA56C122094305303B9DE2093333601926C21E2B39F9E545A"),
  highloadV3(
      "B5EE9C7241021001000228000114FF00F4A413F4BCF2C80B01020120020D02014803040078D020D74BC00101C060B0915BE101D0D3030171B0915BE0FA4030F828C705B39130E0D31F018210AE42E5A4BA9D8040D721D74CF82A01ED55FB04E030020120050A02027306070011ADCE76A2686B85FFC00201200809001AABB6ED44D0810122D721D70B3F0018AA3BED44D08307D721D70B1F0201200B0C001BB9A6EED44D0810162D721D70B15800E5B8BF2EDA2EDFB21AB09028409B0ED44D0810120D721F404F404D33FD315D1058E1BF82325A15210B99F326DF82305AA0015A112B992306DDE923033E2923033E25230800DF40F6FA19ED021D721D70A00955F037FDB31E09130E259800DF40F6FA19CD001D721D70A00937FDB31E0915BE270801F6F2D48308D718D121F900ED44D0D3FFD31FF404F404D33FD315D1F82321A15220B98E12336DF82324AA00A112B9926D32DE58F82301DE541675F910F2A106D0D31FD4D307D30CD309D33FD315D15168BAF2A2515ABAF2A6F8232AA15250BCF2A304F823BBF2A35304800DF40F6FA199D024D721D70A00F2649130E20E01FE5309800DF40F6FA18E13D05004D718D20001F264C858CF16CF8301CF168E1030C824CF40CF8384095005A1A514CF40E2F800C94039800DF41704C8CBFF13CB1FF40012F40012CB3F12CB15C9ED54F80F21D0D30001F265D3020171B0925F03E0FA4001D70B01C000F2A5FA4031FA0031F401FA0031FA00318060D721D300010F0020F265D2000193D431D19130E272B1FB00B585BF03"),
  multisig(
      "B5EE9C7241022B01000418000114FF00F4A413F4BCF2C80B010201200203020148040504DAF220C7008E8330DB3CE08308D71820F90101D307DB3C22C00013A1537178F40E6FA1F29FDB3C541ABAF910F2A006F40420F90101D31F5118BAF2AAD33F705301F00A01C20801830ABCB1F26853158040F40E6FA120980EA420C20AF2670EDFF823AA1F5340B9F2615423A3534E202321220202CC06070201200C0D02012008090201660A0B0003D1840223F2980BC7A0737D0986D9E52ED9E013C7A21C2125002D00A908B5D244A824C8B5D2A5C0B5007404FC02BA1B04A0004F085BA44C78081BA44C3800740835D2B0C026B500BC02F21633C5B332781C75C8F20073C5BD0032600201200E0F02012014150115BBED96D5034705520DB3C82A020148101102012012130173B11D7420C235C6083E404074C1E08075313B50F614C81E3D039BE87CA7F5C2FFD78C7E443CA82B807D01085BA4D6DC4CB83E405636CF0069006027003DAEDA80E800E800FA02017A0211FC8080FC80DD794FF805E47A0000E78B64C00017AE19573FC100D56676A1EC40020120161702012018190151B7255B678626466A4610081E81CDF431C24D845A4000331A61E62E005AE0261C0B6FEE1C0B77746E10230189B5599B6786ABE06FEDB1C6CA2270081E8F8DF4A411C4A05A400031C38410021AE424BAE064F6451613990039E2CA840090081E886052261C52261C52265C4036625CCD8A30230201201A1B0017B506B5CE104035599DA87B100201201C1D020399381E1F0111AC1A6D9E2F81B60940230015ADF94100CC9576A1EC1840010DA936CF0557C160230015ADDFDC20806AB33B50F6200220DB3C02F265F8005043714313DB3CED54232A000AD3FFD3073004A0DB3C2FAE5320B0F26212B102A425B3531CB9B0258100E1AA23A028BCB0F269820186A0F8010597021110023E3E308E8D11101FDB3C40D778F44310BD05E254165B5473E7561053DCDB3C54710A547ABC242528260020ED44D0D31FD307D307D33FF404F404D1005E018E1A30D20001F2A3D307D3075003D70120F90105F90115BAF2A45003E06C2121D74AAA0222D749BAF2AB70542013000C01C8CBFFCB0704D6DB3CED54F80F70256E5389BEB198106E102D50C75F078F1B30542403504DDB3C5055A046501049103A4B0953B9DB3C5054167FE2F800078325A18E2C268040F4966FA52094305303B9DE208E1638393908D2000197D3073016F007059130E27F080705926C31E2B3E630062A2728290060708E2903D08308D718D307F40430531678F40E6FA1F2A5D70BFF544544F910F2A6AE5220B15203BD14A1236EE66C2232007E5230BE8E205F03F8009322D74A9802D307D402FB0002E83270C8CA0040148040F44302F0078E1771C8CB0014CB0712CB0758CF0158CF1640138040F44301E201208E8A104510344300DB3CED54925F06E22A001CC8CB1FCB07CB07CB3FF400F400C9B99895F4"),
  multisigV2(
      "B5EE9C7241021201000495000114FF00F4A413F4BCF2C80B010201620802020120060302016605040159B0C9FE0A00405C00B21633C5804072FFF26208B232C07D003D0032C0325C007E401D3232C084B281F2FFF274201100F1B0CAFB513434FFC04074C1C0407534C1C0407D01348000407448DFDC2385D4449E3D1F1BE94C886654C0AEBCB819C0A900B7806CC4B99B08548C2EBCB81B085FDC2385D4449E3D1F1BE94C886654C0AEBCB819C0A900B7806CC4B99B084C08B0803CB81B8930803CB81B5490EEFCB81B40648CDFE440F880E00143BF74FF6A26869FF8080E9838080EA69838080FA0269000080E8881AAF8280FC11D0C0700C2F80703830CF94130038308F94130F8075006A18127F801A070F83681120670F836A0812BEC70F836A0811D9870F836A022A60622A081053926A027A070F83823A481029827A070F838A003A60658A08106E05005A05005A0430370F83759A001A002CAD033D0D3030171B0925F03E0FA403022D749C000925F03E002D31F0120C000925F04E001D33F01ED44D0D3FF0101D3070101D4D3070101F404D2000101D1288210F718510FBAE30F054443C8500601CBFF500401CB0712CC0101CB07F4000101CA00C9ED540D09029A363826821075097F5DBA8EBA068210A32C59BFBA8EA9F82818C705F2E06503D4D1103410364650F8007F8E8D2178F47C6FA5209132E30D01B3E65B10355034923436E2505413E30D40155033040B0A02E23604D3FF0101D32F0101D3070101D3FF0101D4D1F8285005017002C858CF160101CBFFC98822C8CB01F400F400CB00C97001F90074C8CB0212CA07CBFFC9D01BC705F2E06526F9001ABA5193BE19B0F2E06607F823BEF2E06F44145056F8007F8E8D2178F47C6FA5209132E30D01B3E65B110B01FA02D74CD0D31F01208210F1381E5BBA8E6A82101D0CFBD3BA8E5E6C44D3070101D4217F708E17511278F47C6FA53221995302BAF2E06702A402DE01B312E66C2120C200F2E06E23C200F2E06D5330BBF2E06D01F404217F708E17511278F47C6FA53221995302BAF2E06702A402DE01B312E66C2130D155239130E2E30D0C001030D307D402FB00D1019E3806D3FF0128B38E122084FFBA923024965305BAF2E3F0E205A405DE01D2000101D3070101D32F0101D4D1239126912AE2523078F40E6FA1F2E3EF1EC705F2E3EF20F823BEF2E06F20F823A1546D700E01D4F80703830CF94130038308F94130F8075006A18127F801A070F83681120670F836A0812BEC70F836A0811D9870F836A022A60622A081053926A027A070F83823A481029827A070F838A003A60658A08106E05005A05005A0430370F83759A001A01CBEF2E064F82850030F02B8017002C858CF160101CBFFC98822C8CB01F400F400CB00C97021F90074C8CB0212CA07CBFFC9D0C882109C73FBA2580A02CB1FCB3F2601CB075250CC500B01CB2F1BCC2A01CA000A951901CB07089130E2102470408980188050DB3C111000928E45C85801CB055005CF165003FA0254712323ED44ED45ED479F5BC85003CF17C913775003CB6BCCCCED67ED65ED64747FED11987601CB6BCC01CF17ED41EDF101F2FFC901FB00DB060842026305A8061C856C2CCF05DCB0DF5815C71475870567CAB5F049E340BCF59251F3ADA4AC42"),
  multisigV2Order(
      "B5EE9C7241020C01000376000114FF00F4A413F4BCF2C80B01020162030200C7A1C771DA89A1F48003F0C3A7FE03F0C441AE9380011C2C60DBF0C6DBF0C8DBF0CADBF0CCDBF0CEDBF0D0DBF0D31C45A60E03F0C7A40003F0C9A803F0CBA7FE03F0CDA60E03F0CFA65E03F0D1A803F0D3A3C5F083F085F087F089F08BF08DF08FF091F09303F8D03331D0D3030171B0915BE0FA403001D31F01ED44D0FA4001F861D3FF01F86220D749C0008E16306DF8636DF8646DF8656DF8666DF8676DF8686DF8698E22D30701F863D20001F864D401F865D3FF01F866D30701F867D32F01F868D401F869D1E220C000E30201D33F012282109C73FBA2BAE302028210A762230F070504014ABA8E9BD3070101D1F845521078F40E6FA1F2E06A5230C705F2E06A59DB3CE05F03840FF2F00802FE32F84113C705F2E068F8436E8EF101D30701F86370F864D401F86570F86670F867D32F01F868F848F823BEF2E06FD401F869D200018E99D30701AEF84621B0F2D06BF847A4F867F84601B1F86601DB3C9131E2D1F849F846F845C8F841CF16F84201CBFFF84301CB07F84401CA00CCCBFFF84701CB07F84801CB2FCCC9ED540A06018CE001D30701F843BAF2E069D401F900F845F900BAF2E069D32F01F848BAF2E069D401F900F849F900BAF2E069D20001F2E069D3070101D1F845521078F40E6FA1F2E06A58DB3C0801C83020D74AC0008E23C8708E1A22D7495230D71912CF1622D74A9402D74CD093317F58E2541220E63031C9D0DF840F018B7617070726F76658C705F2F420707F8E19F84578F47C6FA5209B5243C70595317F327001DE9132E201B3E632F2E06AF82512DB3C08026E8F335CED44ED45ED478E983170C88210AFAF283E580402CB1FCB3FCB1F80108050DB3CED67ED65ED64727FED118AED41EDF101F2FFDB030B0902B4F844F2D07002AEF84621B0F2D06BF847A4F867F84601B1F86670C8821082609BF62402CB1FCB3F80108050DB3CDB3CF849F846F845C8F841CF16F84201CBFFF84301CB07F84401CA00CCCBFFF84701CB07F84801CB2FCCC9ED540B0A0180F847F843BA8EB6F84170F849C8821075097F5D580502CB1FCB3FF84201CBFFF84801CB2FF84701CB07F845F90001CBFF13CC128010810090DB3C7FF8649130E20B00888E40C85801CB055004CF1658FA02547120ED44ED45ED479D5BC85003CF17C9127158CB6ACCED67ED65ED64737FED11977001CB6A01CF17ED41EDF101F2FFC901FB00DB0545F8021C"),
  multisigV2Library(
      "B5EE9C72410104010099000114FF00F4A413F4BCF2C80B01037ED33031D0D3030130FA4030ED44F807218103E8F94130F8075003A17FF83B02821012CC03007FF837A08010FB020170C880108306DB3C72FB0688FB0488ED54030202000000888E40C85801CB055004CF1658FA02547120ED44ED45ED479D5BC85003CF17C9127158CB6ACCED67ED65ED64737FED11977001CB6A01CF17ED41EDF101F2FFC901FB00DB05AFD31959"),
  master(
      "FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F31D307D4D101FB00A4C8CB1FCBFFC9ED54"), // partial code
  config("FF00F4A413F4BCF2C80B"), // partial code
  libraryDeployer(
      "B5EE9C7201010601002D000114FF00F4A413F4BCF2C80B0102012002030202D104050006F2F00100053C006000133B511CBEC1BE03FE0020");

  private final String value;

  WalletCodes(final String value) {
    this.value = value;
  }

  public static String getKeyByValue(String value) {
    for (WalletCodes v : WalletCodes.values()) {
      if (v.getValue().equals(value.toUpperCase())) {
        return v.name();
      }
    }
    return null;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return this.getValue();
  }
}
