require 'rubygems'
require 'point'
require 'zonefile'

zonefile = "zone.txt" 
fileCred = "pointHQ_credentials.txt"
username=nil;apitoken=nil;zoneName=nil;zoneId=nil;ttlToUse=nil
fr=File.new(zonefile)
while (line=fr.gets)
	zoneName=line.split(" ").last if line.include? "$ORIGIN" 
	username=line.split(" ")[4] if line.include? "IN SOA"
	ttlToUse=line.split(" ").last if line.include? "$TTL"
end
fr.close
fr=File.new(fileCred)
while (line=fr.gets)
	if line.include? username
		zoneId=fr.gets.split(" ").last
		apitoken=fr.gets.split(" ").last
		break
	end
end
fr.close

puts "Just loaded "+username+" "+apitoken+" "+zoneName+" "+zoneId

Point.username = username
Point.apitoken = apitoken
 
# Find ID
zone = Point::Zone.find(zoneId.to_i)
 
# Load Zonefile
puts "Parsing DNS records"
zf = Zonefile.from_file(zonefile)
 
# Display A-Records
puts "Uploading A records"
zf.a.each do |a_record|
new_record = zone.build_record
new_record.record_type = "A"
zhost = a_record[:name]
if zhost.include? "@"
zhost = zoneName + "."
end
new_record.name = zhost
new_record.ttl=ttlToUse
new_record.data = a_record[:host]
success = new_record.save
puts "#{success} ==> A: #{a_record[:name]} --> #{a_record[:host]}"
end
 
puts "Uploading MX records"
# Display MX-Records
zf.mx.each do |mx_record|
new_record = zone.build_record
new_record.record_type = "MX"
zhost = mx_record[:name]
if zhost.include? "@"
zhost = zoneName + "."
end
new_record.name = zhost
new_record.ttl=ttlToUse
new_record.data = mx_record[:host]
new_record.aux = mx_record[:pri]
success = new_record.save
puts "#{success} ==> MX: #{mx_record[:name]} --> #{mx_record[:host]}"
end
 
puts "Uploading NS records"
# Display NS-Records
zf.ns.each do |ns_record|
unless ns_record[:name] = "@"
new_record = zone.build_record
new_record.record_type = "NS"
new_record.name = ns_record[:name]
new_record.data = ns_record[:host]
success = new_record.save
puts "#{success} ==> NS: #{ns_record[:name]} --> #{ns_record[:host]}"
end
end
 
puts "Uploading CNAME records"
# Display CNAME-Records
zf.cname.each do |cname_record|
new_record = zone.build_record
new_record.record_type = "CNAME"
new_record.name = cname_record[:name]
zhost = cname_record[:name]
if zhost.include? "@"
zhost = zoneName + "."
end
new_record.name = zhost
ztarget = cname_record[:host]
if ztarget.include? "@"
ztarget = zoneName
end
new_record.data = ztarget + "."
success = new_record.save
puts "#{success} ==> CNAME: #{cname_record[:name]} --> #{cname_record[:host]}"
end
 
puts "Uploading TXT records"
# Display TXT-Records
zf.txt.each do |txt_record|
new_record = zone.build_record
new_record.record_type = "TXT"
zhost = txt_record[:name]
if zhost.include? "@"
zhost = zoneName + "."
end
new_record.name = zhost
new_record.ttl=ttlToUse
new_record.data = txt_record[:text]
success = new_record.save
puts "#{success} ==> TXT: #{txt_record[:name]} --> #{txt_record[:text]}"
end
 
puts "Uploading AAAA records"
# Display AAAA-Records
zf.a4.each do |aaaa_record|
new_record = zone.build_record
new_record.record_type = "AAAA"
zhost = aaaa_record[:name]
if zhost.include? "@"
zhost = zoneName + "."
end
new_record.name = zhost
new_record.ttl=ttlToUse
new_record.data = aaaa_record[:host] + "."
new_record.aux = aaaa_record[:pri]
success = new_record.save
puts "#{success} ==> AAAA: #{aaaa_record[:name]} --> #{aaaa_record[:pri]} --> #{aaaa_record[:host]}"
end
 
puts "Uploading SRV records"
# Display SRV-Records
zf.srv.each do |srv_record|
new_record = zone.build_record
new_record.record_type = "SRV"
zhost = srv_record[:name]
if zhost.include? "@"
zhost = zoneName + "."
end
new_record.name = zhost
new_record.ttl=ttlToUse
new_record.data = srv_record[:weight] + " " + srv_record[:port] + " " + srv_record[:host]
new_record.aux = srv_record[:pri]
success = new_record.save
puts "#{success} ==> SRV: #{srv_record[:name]} --> #{srv_record[:pri]} --> #{srv_record[:host]}"
end
puts "Finished!" 
