import React, { useState } from 'react'
import videoLogo from "../assets/video-camera.png"
import { Alert, Button, Card, Label, Progress, TextInput, Textarea } from "flowbite-react";
import axios from "axios"
import toast from 'react-hot-toast';

function VideoUpload() {
    const [selectedFile, setSelectedFile] = useState(null);
    const [progress, setProgress] = useState(0);
    const [uploading, setUploading] = useState(false);
    const [message, setMessage] = useState("");
    const [meta, setMeta] = useState({
        title: "",
        description: "",
    })

    function handleFileChange(event){

        setSelectedFile(event.target.files[0]);
    }

    function formFieldChange(event){
        //console.log(event.target.name);
        //console.log(event.target.value);
        setMeta({
            ...meta,
            [event.target.name]: event.target.value,
        })
    }

    function handleForm(formEvent){
       formEvent.preventDefault();
        
       if(!selectedFile){
        alert("Select File !!");
        return;
       }


       // submit the file to server
        saveVideoToServer(selectedFile,meta);
    }

    function resetForm(){
        setMeta({
            title: "",
            description: "",
        });
        setSelectedFile(null);
        setUploading(false);
        // setMessage("");
    }

    // submit file to server
    async function saveVideoToServer(video, videoMetaData){
        setUploading(true);
        // API call
        try{

            let formData = new FormData();
            formData.append("title", videoMetaData.title);
            formData.append("description", videoMetaData.description);
            formData.append("file",selectedFile);


            let response = await axios.post("http://localhost:8080/api/v1/videos", formData,{
                header: {
                    "Content-Type": "multipart/form-data",
                },
                onUploadProgress: (progressEvent) =>{
                    const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    console.log(progress);
                    setProgress(progress);
                },
            });
            console.log(response);
            setProgress(0);

            setMessage("File Uploaded: " + response.data.videoId);
            setUploading(false);

            toast.success("File uploaded successfully !!");
            resetForm();

        } catch(error){
            console.log(error);
            setMessage("Error in Uploading file");
            setUploading(false);

            toast.error("File not uploaded !!");
        }
    }

    return (
    <div className='text-white'>

        <Card>
            <h1 className='flex justify-center'>Upload Video</h1>

            <div>
            <form noValidate className="space-y-5" onSubmit={handleForm}>

            <div>
                <div className="mb-2 block">
                    <Label htmlFor="title" value="Title" />
                </div>
                <TextInput value={meta.title} onChange={formFieldChange} name='title' placeholder='Enter video title' id='title' required/>
            </div>

            <div className="max-w-md">
            <div className="mb-2 block">
                <Label htmlFor="description" value="Description" />
            </div>
            <Textarea value={meta.description} onChange={formFieldChange} name='description' id="description" placeholder="Video description..." required rows={4} />
            </div>

            <div className="flex items-center space-x-5 justify-center">
                <div className="shrink-0">
                    <img className="h-16 w-16 object-cover rounded-full" src={videoLogo} alt="Video file photo" />
                </div>
                <label className="block">
                    <span className="sr-only">Choose video file</span>
                    <input 
                    name='file'
                    onChange={handleFileChange}
                    type="file" className="block w-full text-sm text-slate-500
            file:mr-4 file:py-2 file:px-4
            file:rounded-full file:border-0
            file:text-sm file:font-semibold
            file:bg-violet-50 file:text-violet-700
            hover:file:bg-violet-100
            " required/>
                </label>
                
            </div>

            <div className="">
                { uploading && (
                <Progress
                progress={progress}
                textLabel="Uploading"
                size={"lg"}
                labelProgress
                labelText 
                />   
                )}
            </div>

            <div className="">
                {message && ( 
                    <Alert color={"success"}
                    onDismiss= { () => {
                        setMessage("");
                    } } >
                    <span className="font-medium" >Success :</span>
                    {message}
                </Alert> )}

            </div>

            <div className='flex justify-center'>
                <Button disabled={uploading} type="submit">Upload</Button>
            </div>

            </form>
            </div>
        </Card>

    </div>
    )
}

export default VideoUpload