require 'open3'

def printCredentials(domain,username,apikey,zoneId)
	@fw2.puts "Domain: "+domain+"\nUsername: "+username+"\nZoneID: "+zoneId+"\nApikey: "+apikey+"\n------------------------------"
end

i=0
file="zone.txt"
fileCred="../pointHQ_credentials.txt"
fw=File.new(file,"w")
domain1="dcsforth.tk."; username1="andros_1@otenet.gr."; apikey1="378312ab-36e0-097f-b496-6bcab9e5d206"; zoneId1 = "166990"
domain2="anondns.cf."; username2="d0xed7@gmail.com.";apikey2="c903e3cd-32ba-0a81-d051-d960e4cf3207"; zoneId2 = "191204"
puts "Printing credentials to "+fileCred
@fw2=File.new(fileCred,"w")
printCredentials(domain1,username1,apikey1,zoneId1)
printCredentials(domain2,username2,apikey2,zoneId2)
@fw2.close
numOfRecords=ARGV[1].to_i
domainToLOAD=ARGV[0].to_i
abort "No input" if ARGV[0]==nil or ARGV[1]==nil
domain=nil;username=nil
if domainToLOAD==1
	puts "Using domain "+domain1
	domain=domain1;username=username1
end
if domainToLOAD==2
	puts "Using domain "+domain2
	domain=domain2;	username=username2
end
abort "Error" if domain==nil

str="$ORIGIN "+domain+"\n$TTL 10\n"+domain+"  IN SOA "+domain+" "+username+" (\n             2015022300 ; zone serial in YYYYMMDDHH format\n             3600  ; refresh (s)\n             120   ; retry (s)\n             86400 ; expiration (s)\n             600   ; max cache (s)\n             )\n"+domain+"  IN  NS  dns4.pointhq.com.\n"+domain+"  IN  NS  dns8.pointhq.com.\n"+domain+"  IN  NS  dns9.pointhq.com.\n"+domain+"  IN  NS  dns10.pointhq.com.\n"+domain+"  IN  NS  dns11.pointhq.com.\n"
fw.puts str
while(i<numOfRecords)
	fw.puts i.to_s+'1b75a1531d150c1894fc0b6b683237193a88069a.'+domain+' 10 IN  TXT  "6ba9bc5eabbe59005b9e01bffcf5ab4dbd3305c44e98efbfe3fa9e0852a67d29020bcf1055907fc330d1c44632b2571503ea780e2c2b3304b87c29151a8efea78181'+i.to_s+'\n'
	i=i+1
end
fw.close
puts "Done! Check "+file
puts `cat #{file}`
