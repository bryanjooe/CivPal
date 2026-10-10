const fs=require('fs');
const data=JSON.parse(fs.readFileSync('supabase/.runtime/live-public.json','utf8'));
for(const key of Object.keys(data)){if(Array.isArray(data[key][0]))data[key]=data[key][0];console.log(key+': '+data[key].length);}
fs.mkdirSync('app/src/test/resources',{recursive:true});
fs.writeFileSync('app/src/test/resources/civic-public-fixture.json',JSON.stringify(data,null,2));
