= SPEEDD Runtime Install Scripts =
The scripts in this directory required to configure storm and kafka as services in Linux:
1. su root
2. For every script do as follows: 
  2.1 Copy the script into /etc/init.d
  2.2 chmod 755 <name> - for all scripts copied in the previous step
  2.3 chconfig --add <script-name>
3. mkdir /var/log/kafka
4. mkdir /var/log/storm

You could run service <service-name> status|stop|start|restart

Example: service storm-nimbus status 