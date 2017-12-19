# PROUD (PRivacy-preservation Of User Discovery)

Installation:
---------------------
cd src/
make

this will create an independent folder "proud_app" which has all needed binaries and libs

Run:
---------------------
cd proud_app/
./proud.sh

then you can navigate in the menu:

Choose from menu:
1. Login
2. Sign up
3. Exit

after login:

Choose from menu:
1. Show Contacts
2. Add New Follower
3. Follow User
4. Send Message to Followed User
5. Delete Follower
6. Unfollow User
7. Exit

Demo Scenario
---------------------
 
- Bob with telephone Number 132 is the owner of a zone in dcsforth.tk.
Alice with telephone Number 345 asked Bob to follow him by sending her cert (./Alice/_myCert.cert)

- Bob has created a TXT domain for Alice by SHA hashing her telephone Number: 51eac6b471a284d3341d8c0c63d0f1a286262a18.dcsforth.tk and he has added as payload his enc(IP)
see dns lookup here: https://mxtoolbox.com/SuperTool.aspx?action=txt%3a51eac6b471a284d3341d8c0c63d0f1a286262a18.dcsforth.tk&run=toolpage

- Alice, who has Bob's cert (./Bob/_myCert.cert), can decrypt his IP and send him a message by opening a socket. Alice will continue retrieving Bob's DNS record every 1 min (pings are dumped in Alice/followedUsersPings.log).

- All information (beside certs) of the users are stored always encrypted in the disk (*.anondns files) with AES128 using their login password.
